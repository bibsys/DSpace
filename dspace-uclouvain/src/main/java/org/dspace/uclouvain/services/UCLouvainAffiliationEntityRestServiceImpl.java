package org.dspace.uclouvain.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Context.Mode;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.uclouvain.core.model.AffiliationEntityRestModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Service to manage an affiliation entity tree. This tree holds information about institutions, institutes, sectors, etc.
 * The service holds a property 'affiliationsEntities' (to store the tree) which can be accessed by other classes with the 'getAffiliationsEntities' method.
 * 
 * @Author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@EnableAsync
public class UCLouvainAffiliationEntityRestServiceImpl implements UCLouvainAffiliationEntityRestService {
    private ItemService itemService;
    private SearchService searchService;
    private Logger logger;
    private List<AffiliationEntityRestModel> affiliationsEntities = new ArrayList<AffiliationEntityRestModel>();

    public UCLouvainAffiliationEntityRestServiceImpl() {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.searchService = SearchUtils.getSearchService();
        this.logger = LogManager.getLogger(UCLouvainAffiliationEntityRestServiceImpl.class);
        this.updateAffiliationEntities();
    }

    // Method to get affiliations entities. Must use the synchronized keyword to avoid concurrency issues.
    public synchronized List<AffiliationEntityRestModel> getAffiliationsEntities() {
        return affiliationsEntities;
    }

    // Method to set affiliations entities. Must use the synchronized keyword to avoid concurrency issues.
    private synchronized void setAffiliationsEntities(List<AffiliationEntityRestModel> affiliationsEntities) {
        this.affiliationsEntities = affiliationsEntities;
    }

    @Async
    public void updateAffiliationEntities() {
        this.logger.info("Starting affiliations entities refresh...");
        Context context = new Context(Mode.READ_ONLY);
        this.processUpdate(context);
        // Important to close each context to avoid memory leaks && cache issues
        context.close();
    }

    /**
     * Triggers an update of the affiliations entities tree.
     * @param context: The current DSpace context.
     */
    private void processUpdate(Context context) {
        try {            
            long startTime;
            long endTime;
            
            startTime = System.currentTimeMillis();
            // Retrieve all OrgUnits from Solr
            List<Item> orgUnits = this.getOrgUnits(context);

            // Convert the list of OrgUnits to a list of AffiliationEntityRestModel 
            List<AffiliationEntityRestModel> modelsList = this.getModelsFromItem(orgUnits);

            // Generate the tree structure of the affiliations
            this.createAffiliationsStructure(modelsList);

            // Set the public property to the new affiliation tree
            this.setAffiliationsEntities(modelsList);
            endTime = System.currentTimeMillis();
            this.logger.info("Refreshed affiliation tree in " + (endTime - startTime) + "ms");
        } catch (Exception e) {
            this.logger.error("Error while processing affiliations entities: " + e);
        }
    }

    /**
     * Creates the tree structure of the affiliations entities using the 'parent' property of the AffiliationEntityRestModel to find the UUID of the parent.
     * @param models: The list of AffiliationEntityRestModel to create the tree structure from.
     */
    private void createAffiliationsStructure(List<AffiliationEntityRestModel> models) {
        for (AffiliationEntityRestModel model: models) {
            if (model.parent != null) {
                AffiliationEntityRestModel parent = models.stream().filter(m -> m.UUID.equals(model.parent)).findFirst().orElse(null);
                if (parent != null) {
                    parent.children.add(model);
                }
            }
        }
    }

    /**
     * Converts a list of Items into a list of AffiliationEntityRestModel.
     * Maps the metadata of the Item to the properties of the model.
     * @param items: The list of Items to convert.
     * @return A list of AffiliationEntityRestModel.
     */
    private List<AffiliationEntityRestModel> getModelsFromItem(List<Item> items) {
        List<AffiliationEntityRestModel> models = new ArrayList<AffiliationEntityRestModel>();
        for (Item item: items) {
            AffiliationEntityRestModel model = new AffiliationEntityRestModel();
            model.UUID = item.getID();
            model.name = itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY);
            model.acronym = itemService.getMetadataFirstValue(item, "oairecerif", "acronym", null, Item.ANY);
            model.type = itemService.getMetadataFirstValue(item, "dc", "type", null, Item.ANY);

            String selectable = itemService.getMetadataFirstValue(item, "organization", "isSelectable", null, Item.ANY);
            // TODO: See if we set true by default if metadata not found.
            model.isSelectable = (selectable != null) ? selectable.equals("true"): true;

            // Extract parent UUID
            MetadataValue parentValue = this.itemService.getMetadata(item, "organization", "parentOrganization", null, Item.ANY).stream().findFirst().orElse(null);
            try {
                if (parentValue != null && parentValue.getAuthority() != null) {
                    model.parent = UUID.fromString(parentValue.getAuthority());
                }
            } catch (Exception e) {
                // LOG error, authority should be a valid UUID string
                this.logger.error("Error while parsing parent UUID of OrgUnit with ID: " + item.getID() + " - error: " + e);
            }
            model.children = new ArrayList<AffiliationEntityRestModel>();
            models.add(model);
        }
        return models;
    }

    /**
     * Retrieve all OrgUnits (affiliations entities) from Solr.
     * @param context: The current DSpace context.
     * @return A list of Items representing the OrgUnits (Affiliations).
     * @throws SearchServiceException
     */
    @SuppressWarnings("rawtypes")
    private List<Item> getOrgUnits(Context context) throws SearchServiceException {
        DiscoverQuery query = this.getOrgUnitQuery();
        List<IndexableObject> results = this.searchService.search(context, query).getIndexableObjects();

        // Conversion from ReloadableEntity to Item
        return results.stream()
            .map(result -> result.getIndexedObject())
            .filter(dso -> dso instanceof DSpaceObject)
            .map(dso -> (DSpaceObject) dso)
            .filter(dso -> dso instanceof Item)
            .map(dso -> (Item) dso)
            .collect(Collectors.toList());
    }

    /**
     * Returns the query to get all OrgUnits from Solr.
     * @return The Solr query.
     */
    private DiscoverQuery getOrgUnitQuery(){
        DiscoverQuery orgUnitQuery = new DiscoverQuery();
        orgUnitQuery.setQuery("search.entitytype:\"OrgUnit\" && search.resourcetype: \"Item\" && discoverable:\"true\"");
        // Set to configuration variable
        orgUnitQuery.setMaxResults(1000);
        return orgUnitQuery;
    }
}
