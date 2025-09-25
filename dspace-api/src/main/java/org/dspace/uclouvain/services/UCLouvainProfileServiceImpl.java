/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import static java.util.Optional.ofNullable;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.ws.rs.NotFoundException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableInProgressSubmission;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexableWorkflowItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to operate Profile items.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainProfileServiceImpl implements UCLouvainProfileService {
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

    private static final String PROFILE_ENTITY_TYPE = "Person";

    /**
     * Find a profile item using an FGS identifier. Returns null if nothing is found.
     * 
     * @param context The current DSpace context.
     * @param fgs The identifier to use in order to find the right profile.
     * @return Returns the profile that has the given identifier, null otherwise.
     */
    public Item findById(Context context, String fgs) throws Exception {
        // Add the fgs as a filter
        return findOneByAttribute(context, "person.identifier.fgs:" + fgs);
    }

    /**
     * Find a profile item using an email. Returns null if nothing is found.
     * 
     * @param context The current DSpace context.
     * @param email The email to use in order to find the right profile.
     * @return Returns the profile that has the given email, null otherwise.
     */
    public Item findByEmail(Context context, String email) throws Exception {
        // Add the email as a filter
        return findOneByAttribute(context, "person.email:" + email);
    }

    /**
     * Find a profile item using a given attribute filter. Returns null if nothing is found.
     * 
     * @param context The current DSpace context.
     * @param attributeFilter The filter to use to find a specific profile item.
     * @return Returns the profile that passes the given filter, null if nothing found.
     */
    private Item findOneByAttribute(Context context, String attributeFilter) throws Exception {
        DiscoverQuery dq = new DiscoverQuery();
        // Take only archived items.
        dq.addDSpaceObjectFilter(IndexableItem.TYPE);
        dq.addFilterQueries("search.entitytype:" + PROFILE_ENTITY_TYPE);
        // Add the attribute filter
        dq.addFilterQueries(attributeFilter);
        dq.setMaxResults(1);
        // Convert the search result into a list of item and only keep the first one.
        return searchService.search(context, dq)
            .getIndexableObjects()
            .stream()
            .map(indexableObject -> ((IndexableItem) indexableObject).getIndexedObject())
            .findFirst().orElse(null);
    }

    /**
     * For a given profile, retrieve all the linked publications that uses this profile has an author.
     * 
     * @param context The current DSpace context.
     * @param profile The profile to find publications for.
     * @return Any publication that references this profile has an author.
     */
    public List<Item> findLinkedPublications(Context context, Item profile) throws Exception {
        DiscoverQuery dq = new DiscoverQuery();

        dq.addDSpaceObjectFilter(IndexableWorkspaceItem.TYPE);
        dq.addDSpaceObjectFilter(IndexableWorkflowItem.TYPE);
        dq.addDSpaceObjectFilter(IndexableItem.TYPE);
        dq.addFilterQueries("search.entitytype:Publication");
        // Keep only publications that have the profile id as authority of metadata.
        dq.addFilterQueries("author_authority:\"" + profile.getID() + "\"");
        return searchService.search(context, dq)
            .getIndexableObjects()
            .stream()
            .map((indexableObject) -> {
                return (indexableObject instanceof IndexableItem)
                    ? ((IndexableItem) indexableObject).getIndexedObject()
                    : ((IndexableInProgressSubmission) indexableObject).getIndexedObject().getItem();
            })
            .collect(Collectors.toList());
    }

    /**
     * Create an empty profile item with an fgs identifier.
     * 
     * @param context The current DSpace context.
     * @param fgs The unique fgs identifier to give to the profile item.
     */
    public Item createEmptyProfile(Context context, String fgs) throws Exception {
        Collection profileCollection = getProfileCollection(context);
        WorkspaceItem workspaceItem = workspaceItemService.create(context, profileCollection, true);
        Item profile = workspaceItem.getItem();

        // Add the fgs identifier to the profile.
        itemService.addSecuredMetadata(context, profile, "person", "identifier", "fgs", null, fgs, null, 0, 1);
        profile = installItemService.installItem(context, workspaceItem);

        return profile;
    }

    /**
     * Get the collection that stores the profile items.
     * @param context The current DSpace context.
     * @return A collection that stores profile items or null if not found.
     * @throws SearchServiceException
     */
    private Collection getProfileCollection(Context context) throws SearchServiceException {
        return ofNullable(collectionService.findAllCollectionsByEntityType(context, PROFILE_ENTITY_TYPE).get(0))
            .orElseThrow(() -> new NotFoundException("No collection for " + PROFILE_ENTITY_TYPE + " entity types."));
    }
}
