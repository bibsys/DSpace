/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.impl;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.content.authority.Choices.CF_UNSET;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.services.ConfigurationService;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.model.exceptions.PublicationSetAuthorException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.export.services.UCLouvainExportService;
import org.dspace.uclouvain.services.PublicationService;
import org.dspace.uclouvain.services.UCLouvainProfileService;
import org.dspace.uclouvain.services.queryFilters.SolrQueryFiltersFactory;
import org.dspace.uclouvain.services.queryFilters.SolrSortOptionFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PublicationServiceImpl implements PublicationService {

    @Autowired
    ItemService itemService;
    @Autowired
    SearchService searchService;
    @Autowired
    EPersonService ePersonService;
    @Autowired
    ResearcherProfileService researcherProfileService;
    @Autowired
    ConfigurationService configService;
    @Autowired
    GroupService groupService;
    @Autowired
    UCLouvainProfileService uclouvainProfileService;

    // PUBLIC METHODS ==================================================================================================
    public PublicationAuthor setAuthor(Context context, Publication publication, PublicationAuthor author)
            throws PublicationSetAuthorException {
        Item item = publication.getItem();
        try {
            String authority = (author.getAuthority() != null)
                ? author.getAuthority().getItemId().toString()
                : null;
            int confidence = isNotEmpty(authority) ? CF_ACCEPTED : CF_UNSET;
            int place = author.getPlace();

            MetadataSetter setter = (field, value, auth, conf) ->
                    itemService.setMetadataInPlace(context, item, field, null, value, auth, place, conf);

            setter.set(Publication.AUTHOR_NAME_FIELD, author.getName(), authority, confidence);
            setter.set(Publication.AUTHOR_EMAIL_FIELD, author.getEmail(), authority, confidence);
            setter.set(Publication.AUTHOR_FGS_FIELD, author.getFgs(), authority, confidence);
            setter.set(Publication.AUTHOR_ROLE_FIELD, author.getRole(), null, CF_UNSET);
            if (author.getOrcidID() != null) {
                setter.set(Publication.AUTHOR_ORCID_FIELD, author.getOrcidID(), authority, confidence);
            }
            if (author.getInstitution() != null) {
                setter.set(Publication.AUTHOR_INSTITUTION_FIELD, author.getInstitution(), null, confidence);
            }
            return author;
        } catch (Exception e) {
            throw new PublicationSetAuthorException(item, author);
        }
    }

    public Stream<Publication> findByAuthors(
        Context context,
        List<Pair<String, String>> identifiers,
        Map<String, String> filters,
        UCLouvainExportService.SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws SearchServiceException {
        if (identifiers == null || identifiers.isEmpty()) {
            return Stream.empty();
        }
        // Convert filters to Solr query filters
        List<String> queryFilters = convertQueryFilters(filters);
        String sortField = convertSortOption(sort);
        // We will normalize identifiers list.
        // Specific author identifier (fgs, orcid, ...) should reference an existing researcher profile.
        // If we found a matching profile for this identifier, we will replace initial specific identifier by a
        // normalized 'uuid' identifier.
        List<Pair<String, String>> normalizedIdentifiers = identifiers.stream()
            .flatMap(pair -> {
                if (Objects.equals("fgs", pair.getLeft())) {
                    Item profile = uclouvainProfileService.findById(context, pair.getRight());
                    // DEV-NOTE : returning empty stream == removing this identifier from list
                    return (profile != null) ? Stream.of(Pair.of("uuid", profile.getID().toString())) : Stream.empty();
                } else if (Objects.equals("orcid", pair.getLeft())) {
                    Item profile = uclouvainProfileService.findByOrcid(context, pair.getRight());
                    return (profile != null) ? Stream.of(Pair.of("uuid", profile.getID().toString())) : Stream.empty();
                } else {
                    return Stream.of(pair);
                }
            })
            .toList();
        // Validate normalized identifiers
        // At this time, we should only have "uuid" or "name" identifier type in the list.
        // If another identifier type is found, generate a "fail-fast" error
        Set<String> validTypes = Set.of("uuid", "name");
        for (Pair<String, String> identifier : normalizedIdentifiers) {
            if (!validTypes.contains(identifier.getLeft())) {
                throw new SearchServiceException("Unsupported identifier type :: " + identifier.getLeft());
            }
        }
        // Build query based on normalized identifiers
        String query = normalizedIdentifiers.stream()
            .map(identifier -> switch (identifier.getLeft()) {
                case "uuid" -> "isAuthorOfPublication:\"%s\"".formatted(identifier.getRight());
                case "name" -> "author_keyword:\"%s\"".formatted(identifier.getRight());
                default -> ""; // should never happen... but switch need a default :(
            })
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(" OR "));
        return StringUtils.isNotEmpty(query)
            ? findPublications(context, query, queryFilters, sortField, direction)
            : Stream.empty();
    }

    @Override
    public Stream<Publication> findByAffiliationNames(
        Context context,
        List<String> affiliationNames,
        Map<String, String> filters,
        UCLouvainExportService.SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws SearchServiceException {
        if (affiliationNames == null || affiliationNames.isEmpty()) {
            return Stream.empty();
        }
        List<String> queryFilters = convertQueryFilters(filters);
        String sortField = convertSortOption(sort);
        String queryField = "oairecerif.affiliation.orgunitDepartment";
        String query = affiliationNames.stream()
            .map(name -> String.format("%s:\"%s\"", queryField, ClientUtils.escapeQueryChars(name)))
            .collect(Collectors.joining(" OR "));
        return findPublications(context, query, queryFilters, sortField, direction);
    }

    @Override
    public Stream<Publication> findByAffiliationUUIDs(
        Context context,
        List<String> affiliationUUIDs,
        boolean includeDescendant,
        Map<String, String> filters,
        UCLouvainExportService.SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws SearchServiceException {
        if (affiliationUUIDs == null || affiliationUUIDs.isEmpty()) {
            return Stream.empty();
        }
        List<String> queryFilters = convertQueryFilters(filters);
        String sortField = convertSortOption(sort);
        String queryField = (includeDescendant)
            ? "isHierarchicalOrgUnitOfPublication"
            : "isOrgUnitOfPublication";
        String query = affiliationUUIDs.stream()
            .map(uuid -> String.format("%s:\"%s\"", queryField, uuid))
            .collect(Collectors.joining(" OR "));
        return findPublications(context, query, queryFilters, sortField, direction);
    }

    @Override
    public Stream<Publication> findByFunding(
        Context context,
        String fundingOrg,
        String fundingProg,
        Map<String, String> filters,
        UCLouvainExportService.SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws SearchServiceException {
        if (isNotBlank(fundingProg)) {
            filters.put("fundingProgram", fundingProg);
        }
        List<String> queryFilters = convertQueryFilters(filters);
        String sortField = convertSortOption(sort);
        String query = "funding.organization:\"%s\"".formatted(ClientUtils.escapeQueryChars(fundingOrg));
        return findPublications(context, query, queryFilters, sortField, direction);
    }

    @Override
    public Stream<Publication> findPublications(
        Context context,
        String query,
        Map<String, String> filterQueries,
        UCLouvainExportService.SortOption sort,
        DiscoverQuery.SORT_ORDER sortDirection
    ) throws SearchServiceException {
        List<String> filters = convertQueryFilters(filterQueries);
        String sortOption = convertSortOption(sort);
        return findPublications(context, query, filters, sortOption, sortDirection);
    }

    private Stream<Publication> findPublications(
        Context context,
        String query,
        List<String> filterQueries,
        String sortField,
        DiscoverQuery.SORT_ORDER sortDirection
    ) throws SearchServiceException {
        DiscoverQuery dq = new DiscoverQuery();
        dq.addDSpaceObjectFilter(IndexableItem.TYPE);
        if (StringUtils.isNotBlank(sortField)) {
            sortDirection = (sortDirection != null) ? sortDirection : DiscoverQuery.SORT_ORDER.asc;
            dq.setSortField(sortField, sortDirection);
        }
        dq.setQuery(query);
        dq.setMaxResults(SearchService.MAX_RESULT);
        if (filterQueries != null) {
            filterQueries.forEach(dq::addFilterQueries);
        }
        DiscoverResult searchResult = searchService.search(context, dq);
        return searchResult.getIndexableObjects()
            .stream()
            .map(indexableObject -> buildPublication(((IndexableItem) indexableObject).getIndexedObject()))
            .filter(Objects::nonNull);
    }

    public boolean isAuthorOfPublication(Context context, Item item) throws SQLException, AuthorizeException {
        EPerson user = context.getCurrentUser();
        Publication publication = buildPublication(item);
        if (user == null || publication == null) {
            return false;
        }
        ResearcherProfile profile = researcherProfileService.findById(context, user.getID());
        return publication.getAuthors().stream()
            .map(PublicationAuthor::getAuthority)
            .filter(Objects::nonNull)
            .anyMatch(authorAuthority -> Objects.equals(authorAuthority.getItemId(), profile.getItemId()));
    }


    public boolean authorizeWithdrawItem(Context context, Item item) {
        try {
            Publication publication = PublicationFactory.build(item);
            // First of all, determine if this publication is 'withdrawable', if not, no need extra check.
            if (!publication.isWithdrawable()) {
                return false;
            }
            // To determine if the current logged user can withdraw this publication, we will check
            //   1) if user has manager rights
            //   2) if user is submitter of the publication
            //   3) if user is owner of the publication (DSpace basic behavior)
            //   4) if user is author of the publication
            EPerson user = context.getCurrentUser();
            if (user == null) {
                return false;
            }
            return ePersonService.isOwnerOfItem(user, item)
                || Objects.equals(item.getSubmitter(), user)
                || isManager(context, user)
                || isAuthorOfPublication(context, item);
        } catch (InvalidModelEntityTypeException | SQLException | AuthorizeException e) {
            return false;
        }
    }

    // PRIVATE METHODS =================================================================================================
    private Publication buildPublication(Item item) {
        try {
            return PublicationFactory.build(item);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isManager(Context context, EPerson user) throws SQLException {
        String[] managerGroups = configService.getArrayProperty("uclouvain.feature.roles.manager", new String[] {});
        return groupService.isMember(context, user, managerGroups);
    }


    // CONVERTING QUERY FILTERS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private List<String> convertQueryFilters(Map<String, String> filters) {
        return (filters == null || filters.isEmpty())
            ? Collections.emptyList()
            : filters
                .entrySet().stream()
                .map(entry -> {
                    try {
                        return SolrQueryFiltersFactory.build(entry.getKey()).parse(entry.getValue());
                    } catch (ParseException e) {
                        return "%s:\"%s\"".formatted(entry.getKey(), entry.getValue());
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    private String convertSortOption(UCLouvainExportService.SortOption option) {
        return SolrSortOptionFactory.build(option.toString()).getSortField();
    }

    @FunctionalInterface
    private interface MetadataSetter {
        void set(String field, String value, String auth, int confidence) throws Exception;
    }
}
