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
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;
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
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.services.ConfigurationService;
import org.dspace.uclouvain.core.model.exceptions.PublicationSetAuthorException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.core.utils.CleanIdentifierFields;
import org.dspace.uclouvain.core.utils.IdentifierNormalizer;
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
    ResearcherProfileService researcherProfileService;
    @Autowired
    ConfigurationService configService;
    @Autowired
    UCLouvainProfileService uclouvainProfileService;

    private CleanIdentifierFields cleanIdentifierFields;

    @PostConstruct
    private void loadCleanIdentifierFields() {
        cleanIdentifierFields = new CleanIdentifierFields(configService);
    }

    // SEARCHING METHODS ===============================================================================================
    @Override
    public List<Publication> findByIdentifier(Context context, String metadataField, String rawValue)
        throws SearchServiceException {
        IdentifierNormalizer normalizer = cleanIdentifierFields.normalizerFor(metadataField)
            .orElseThrow(() -> new IllegalArgumentException(
                "'%s' is not listed in '%s': no canonical form is indexed for it"
                    .formatted(metadataField, CleanIdentifierFields.PROPERTY)));
        List<String> forms = normalizer.normalize(rawValue);
        if (forms.isEmpty()) {
            return List.of();
        }
        String query = CleanIdentifierFields.solrField(metadataField) + ":(" + forms.stream()
            .map(form -> "\"" + ClientUtils.escapeQueryChars(form) + "\"")
            .collect(Collectors.joining(" OR ")) + ")";

        DiscoverQuery dq = new DiscoverQuery();
        dq.addDSpaceObjectFilter(IndexableItem.TYPE);
        dq.setQuery(query);
        dq.setIncludeNotDiscoverableOrWithdrawn(true);
        dq.setSortField("dc.date.accessioned_dt", DiscoverQuery.SORT_ORDER.asc);
        dq.setMaxResults(SearchService.MAX_RESULT);
        return toPublications(searchService.search(context, dq)).toList();
    }

    @Override
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
            .flatMap(pair -> switch (pair.getLeft()) {
                case "fgs" -> asUuidIdentifier(uclouvainProfileService.findByFGS(context, pair.getRight()));
                case "orcid" -> asUuidIdentifier(uclouvainProfileService.findByOrcid(context, pair.getRight()));
                default -> Stream.of(pair);
            })
            .toList();
        if (normalizedIdentifiers.isEmpty()) {
            return Stream.empty();
        }
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
                case "name" -> "author_keyword:\"%s\"".formatted(ClientUtils.escapeQueryChars(identifier.getRight()));
                default -> throw new IllegalStateException("Unsupported identifier type :: " + identifier.getLeft());
            })
            .collect(Collectors.joining(" OR "));
        return findPublications(context, query, queryFilters, sortField, direction);
    }

    /** A researcher profile item as a {@code uuid} author identifier; nothing when the profile was not found. */
    private static Stream<Pair<String, String>> asUuidIdentifier(Item profile) {
        return (profile == null) ? Stream.empty() : Stream.of(Pair.of("uuid", profile.getID().toString()));
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
        // never write into the caller's map, and accept a null one like the other finders
        Map<String, String> allFilters = new HashMap<>(filters != null ? filters : Map.of());
        if (isNotBlank(fundingProg)) {
            allFilters.put("fundingProgram", fundingProg);
        }
        List<String> queryFilters = convertQueryFilters(allFilters);
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
        return toPublications(searchService.search(context, dq));
    }

    /** The publications of a search result; indexed objects that are not publications are skipped. */
    private Stream<Publication> toPublications(DiscoverResult result) {
        return result.getIndexableObjects()
            .stream()
            .map(indexableObject -> buildPublication(((IndexableItem) indexableObject).getIndexedObject()))
            .filter(Objects::nonNull);
    }

    // SETTER METHODS ==================================================================================================
    @Override
    public PublicationAuthor setAuthor(
        Context context, Publication publication, PublicationAuthor author, boolean override
    ) throws PublicationSetAuthorException {
        Item item = publication.getItem();
        try {
            String authority = (author.getAuthority() != null)
                ? author.getAuthority().getItemId().toString()
                : null;
            int confidence = isNotEmpty(authority) ? CF_ACCEPTED : CF_UNSET;
            int place = author.getPlace();

            MetadataSetter setter = (field, value, auth, conf) -> {
                // Don't set metadata if override is turned off and if:
                //  - The item has already a metadata for the given place and field AND:
                //      -> The value of this metadata is not equal to the value of the new metadata.
                //      -> The value of this metadata is not equal to a placeholder.
                String currentValue = itemService.getMetadata(item, field, place);
                if (
                    currentValue != null
                        && !Objects.equals(currentValue, value)
                        && !Objects.equals(currentValue, PLACEHOLDER_PARENT_METADATA_VALUE)
                        && !override
                ) {
                    return;
                }
                if (StringUtils.isBlank(value)) {
                    itemService.setMetadataInPlace(
                        context, item, field, null, PLACEHOLDER_PARENT_METADATA_VALUE, null, place, CF_UNSET);
                } else {
                    itemService.setMetadataInPlace(
                        context, item, field, null, value, auth, place, conf);
                }
            };

            setter.set(Publication.AUTHOR_NAME_FIELD, author.getName(), authority, confidence);
            setter.set(Publication.AUTHOR_EMAIL_FIELD, author.getEmail(), authority, confidence);
            setter.set(Publication.AUTHOR_FGS_FIELD, author.getFgs(), authority, confidence);
            setter.set(Publication.AUTHOR_ROLE_FIELD, author.getRole(), null, CF_UNSET);
            setter.set(Publication.AUTHOR_ORCID_FIELD, author.getOrcidID(), authority, confidence);
            setter.set(Publication.AUTHOR_INSTITUTION_FIELD, author.getInstitution(), null, confidence);
            return author;
        } catch (Exception e) {
            throw new PublicationSetAuthorException(item, author);
        }
    }

    // QUESTION METHODS ================================================================================================
    @Override
    public boolean isAuthorOfPublication(Context context, Item item) throws SQLException, AuthorizeException {
        EPerson user = context.getCurrentUser();
        Publication publication = buildPublication(item);
        if (user == null || publication == null) {
            return false;
        }
        ResearcherProfile profile = researcherProfileService.findById(context, user.getID());
        if (profile == null) {
            return false;
        }
        return publication.getAuthors().stream()
            .map(PublicationAuthor::getAuthority)
            .filter(Objects::nonNull)
            .anyMatch(authorAuthority -> Objects.equals(authorAuthority.getItemId(), profile.getItemId()));
    }

    // PRIVATE METHODS =================================================================================================
    private Publication buildPublication(Item item) {
        try {
            return PublicationFactory.build(item);
        } catch (Exception ignored) {
            return null;
        }
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
        return (option != null)
            ? SolrSortOptionFactory.build(option.toString()).getSortField()
            : null;
    }

    @FunctionalInterface
    private interface MetadataSetter {
        void set(String field, String value, String auth, int confidence) throws Exception;
    }
}
