/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.content.authority.Choices.CF_UNSET;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableInProgressSubmission;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.profile.ResearcherProfile;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.OrgUnit;
import org.dspace.uclouvain.profileIngester.exceptions.IDMCheckException;
import org.dspace.uclouvain.profileIngester.services.IDMPersonValidityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to operate Profile items.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainProfileServiceImpl implements UCLouvainProfileService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(UCLouvainProfileServiceImpl.class);

    @Autowired
    protected ItemService itemService;
    @Autowired
    private WorkspaceItemService workspaceItemService;
    @Autowired
    private CollectionService collectionService;
    @Autowired
    private InstallItemService installItemService;
    @Autowired
    private SearchService searchService;
    @Autowired
    private EPersonService ePersonService;
    @Autowired
    private IDMPersonValidityService idmService;
    @Autowired
    private OrgUnitService orgUnitService;

    private String defaultInstitutionAcronym = DSpaceServicesFactory
            .getInstance()
            .getConfigurationService()
            .getProperty("uclouvain.profile.default-institution.acronym");

    private static final String PROFILE_ENTITY_TYPE = "Person";

    // IMPLEMENTED FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public Item findByFGS(Context context, String fgs) {
        return findOneByAttribute(context, formatFilter(ResearcherProfile.FGS_FIELD, fgs));
    }

    public Item findByEmail(Context context, String email) {
        return findOneByAttribute(context, formatFilter(ResearcherProfile.OFFICIAL_EMAIL_FIELD, email));
    }

    public Item findByOrcid(Context context, String orcid) {
        return findOneByAttribute(context, formatFilter(ResearcherProfile.ORCID_FIELD, orcid));
    }

    public Item findByIdentifiers(Context context, String uuid, String fgs, String email) {
        String query = null;
        if (!StringUtils.isBlank(uuid)) {
            query = formatFilter("search.resourceid", uuid);
        }
        if (!StringUtils.isBlank(fgs)) {
            query = formatFilter(ResearcherProfile.FGS_FIELD, fgs);
        }
        if (!StringUtils.isBlank(email)) {
            query = formatFilter(ResearcherProfile.OFFICIAL_EMAIL_FIELD, email);
        }
        if (query == null) {
            throw new IllegalArgumentException("At least one search criteria is required");
        }

        // EXECUTE SOLR QUERY
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries("dspace.entity.type:" + PROFILE_ENTITY_TYPE);
        discoverQuery.setMaxResults(1);
        discoverQuery.setQuery(query);
        try {
            DiscoverResult result = searchService.search(context, discoverQuery);
            return result.getIndexableObjects()
                .stream()
                .map(indexableObject -> ((IndexableItem) indexableObject).getIndexedObject())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        } catch (SearchServiceException sse) {
            return null;
        }
    }

    public List<Item> findByName(Context context, String authorName) {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setQuery("%s:\"%s\"".formatted("dc.title", authorName));
        discoverQuery.addFilterQueries("dspace.entity.type:" + PROFILE_ENTITY_TYPE);
        discoverQuery.setMaxResults(SearchService.MAX_RESULT);
        try {
            DiscoverResult result = searchService.search(context, discoverQuery);
            return result.getIndexableObjects()
                .stream()
                .map(indexableObject -> ((IndexableItem) indexableObject).getIndexedObject())
                .filter(Objects::nonNull)
                .toList();
        } catch (SearchServiceException sse) {
            return Collections.emptyList();
        }
    }

    public List<Item> findLinkedPublications(Context context, Item profile) {
        if (itemService.getEntityType(profile).equals(PROFILE_ENTITY_TYPE)) {
            throw new IllegalArgumentException("`profile` parameter isn't a valid Person entity type");
        }
        DiscoverQuery dq = new DiscoverQuery();
        dq.addDSpaceObjectFilter(IndexableWorkspaceItem.TYPE);
        dq.addDSpaceObjectFilter(IndexableWorkflowItem.TYPE);
        dq.addDSpaceObjectFilter(IndexableItem.TYPE);
        dq.addFilterQueries("search.entitytype:Publication");
        dq.addFilterQueries("author_authority:\"" + profile.getID() + "\"");
        dq.setMaxResults(SearchService.MAX_RESULT);
        try {
            return searchService.search(context, dq)
                .getIndexableObjects()
                .stream()
                .map((indexableObject) -> (indexableObject instanceof IndexableItem)
                        ? ((IndexableItem) indexableObject).getIndexedObject()
                        : ((IndexableInProgressSubmission<?>) indexableObject).getIndexedObject().getItem())
                .toList();
        } catch (SearchServiceException ignored) {
            return Collections.emptyList();
        }
    }

    public Item createEmptyProfile(Context context, String fgs) throws Exception {
        return createEmptyProfile(context, fgs, true);
    }

    public Item createEmptyProfile(Context context, String fgs, boolean addDefaultInstitution) throws Exception {
        Collection profileCollection = getProfileCollection(context);
        WorkspaceItem workspaceItem = workspaceItemService.create(context, profileCollection, true);
        Item profile = workspaceItem.getItem();
        itemService.addSecuredMetadata(context, profile, "person", "identifier", "fgs", null, fgs, null, CF_UNSET, 1);
        if (addDefaultInstitution) {
            OrgUnit defaultInstitution = getDefaultProfileInstitution(context);
            if (defaultInstitution != null) {
                itemService.addMetadata(
                    context, profile,
                    "person", "affiliation", "institution",
                    null, defaultInstitution.getAcronym(), defaultInstitution.getID().toString(), CF_ACCEPTED
                );
            }
        }
        return installItemService.installItem(context, workspaceItem);
    }

    public Item createNewProfile(Context context, EPerson person) throws Exception {
        String fgs = ePersonService.getMetadataFirstValue(person, "eperson", "identifier", "fgs", null);
        if (fgs == null) {
            throw new NoSuchElementException("Missing FGS identifier (employeeNumber) EPerson#" + person.getID());
        }
        // Retrieve all the idm entries of the logged person and check if we can create a profile.
        List<Integer> idmEntries = ePersonService.getMetadata(person, "eperson", "idm", "id", null)
            .stream()
            .map(mv -> Integer.parseInt(mv.getValue()))
            .toList();
        try {
            boolean idmValid = idmService.isPersonIDMValid(idmEntries);
            if (!idmValid) {
                log.info("Canceled profile creation for fgs=['" + fgs + "'] :: no IDM entry is valid.");
                return null;
            }
        } catch (IDMCheckException idme) {
            log.info("Canceled profile creation for fgs=['" + fgs + "'] :: {}", idme.getMessage());
            return null;
        }

        // Create an empty profile with the fgs and complete with additional metadata:
        //   * email
        //   * concatenation of first and last name to create 'dc.title'.
        Item profile = createEmptyProfile(context, fgs);
        String email = person.getEmail();
        log.debug("Found person email form EPerson metadata: " + email);
        itemService.addSecuredMetadata(context, profile, "person", "email", "official", null, email, null, 0, 1);
        itemService.addSecuredMetadata(context, profile, "person", "email", null, null, email, null, 0, 1);

        String fullName = Stream.of(person.getLastName(), person.getFirstName())
            .filter(StringUtils::isNotEmpty)
            .collect(Collectors.joining(", "));
        log.debug("Found person fullname form EPerson metadata: " + fullName);
        if (StringUtils.isNotBlank(fullName)) {
            itemService.addSecuredMetadata(context, profile, "crisrp", "name", null, null, fullName, null, 0, 1);
            itemService.addSecuredMetadata(context, profile, "dc", "title", null, null, fullName, null, 0, 0);
        }

        List<String> affiliations = ePersonService.getMetadata(person, "eperson", "affiliation", null, null)
            .stream()
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
        if (affiliations != null) {
            // Try to find a matching affiliation item for the affiliations stored in the person.
            OrgUnit mainAffiliation = orgUnitService.findFirstByName(context, affiliations);
            if (mainAffiliation != null) {
                // DEV_NOTE: Use setMetadata here to clear the default institution.
                itemService.setMetadataInPlace(
                    context, profile,
                    "person.affiliation.department",
                    null, mainAffiliation.getTitle(), mainAffiliation.getID().toString(), 0, CF_ACCEPTED
                );
                OrgUnit institution = mainAffiliation.getParentUniversity();
                itemService.setMetadataInPlace(
                    context, profile,
                    "person.affiliation.institution",
                    null, institution.getAcronym(), institution.getID().toString(), 0, CF_ACCEPTED
                );
            }
        }
        return profile;
    }

    // PRIVATE FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    private String formatFilter(String field, String value) {
        return "%s:\"%s\"".formatted(field, value);
    }

    /**
     * Find a profile item using a given attribute filter.
     * @param context The current DSpace context.
     * @param attributeFilter The filter to use to find a specific profile item.
     * @return Returns the profile that passes the given filter, null if nothing found.
     */
    private Item findOneByAttribute(Context context, String attributeFilter) {
        DiscoverQuery dq = new DiscoverQuery();
        dq.addDSpaceObjectFilter(IndexableItem.TYPE);
        dq.addFilterQueries("search.entitytype:" + PROFILE_ENTITY_TYPE);
        dq.addFilterQueries(attributeFilter);
        dq.setMaxResults(1);
        try {
            return searchService.search(context, dq)
                .getIndexableObjects()
                .stream()
                .map(indexableObject -> ((IndexableItem) indexableObject).getIndexedObject())
                .findFirst()
                .orElse(null);
        } catch (SearchServiceException ignored) {
            return null;
        }
    }

    /**
     * Get the collection that stores the profile items.
     * @param context The current DSpace context.
     * @return A collection that stores profile items or null if not found.
     * @throws SearchServiceException if search failed
     * @throws NoSuchElementException if no collection could be found
     */
    private Collection getProfileCollection(Context context) throws SearchServiceException, NoSuchElementException {
        return collectionService
            .findAllCollectionsByEntityType(context, PROFILE_ENTITY_TYPE)
            .stream()
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("No collection for " + PROFILE_ENTITY_TYPE + " entity type"));
    }

    private OrgUnit getDefaultProfileInstitution(Context context) {
        // The default institution could be null if the OrgUnits have not been init yet.
        try {
            return orgUnitService.findByName(
                new Context(), defaultInstitutionAcronym, null, null, null
            );
        } catch (Exception e) {
            log.error("Could search for default profile institution '" + defaultInstitutionAcronym + "'", e);
            return null;
        }
    }


}
