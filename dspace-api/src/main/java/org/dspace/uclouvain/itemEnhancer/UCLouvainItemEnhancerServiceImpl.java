/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.uclouvain.itemEnhancer.dao.UCLouvainItemEnhancerDAO;
import org.dspace.uclouvain.itemEnhancer.enhancers.MetadataEnhancer;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Service to handle the custom enhancement system.
 * This class holds the configuration for the feature, describing which item must be enhanced
 * and under which conditions.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainItemEnhancerServiceImpl implements UCLouvainItemEnhancerService {

    private Logger logger = LogManager.getLogger(UCLouvainItemEnhancerServiceImpl.class);

    // Defines the maximum number of items to retrieve at the same time from the database using
    // the 'getItemsToEnhance' method.
    private Integer pullLimit = 1000;

    @Autowired
    private UCLouvainItemEnhancerDAO uclouvainItemEnhancerDAO;

    private List<MetadataEnhancer<Object>> enhancers = new ArrayList<>();
    // Map containing all enhancers sorted by entityType and supported action.
    // First layer is the entityType, second layer is the action.
    private Map<String, Map<String, List<MetadataEnhancer<Object>>>> enhancersMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // Build a map using the loaded enhancers for fast access.
        for (MetadataEnhancer<Object> enhancer : enhancers) {
            String entityType = enhancer.getSupportedEntityType();
            String action = enhancer.getSupportedAction();

            Map<String, List<MetadataEnhancer<Object>>> actionMap =
                enhancersMap.getOrDefault(entityType, new HashMap<>());
            List<MetadataEnhancer<Object>> enhancerList =
                actionMap.getOrDefault(action, new ArrayList<>());
            enhancerList.add(enhancer);

            actionMap.putIfAbsent(action, enhancerList);
            enhancersMap.putIfAbsent(entityType, actionMap);
        }
    }

    /**
     * Given a specific item and its entity type, this method will add an entry to the database.
     * If the item is already present in the database, the existing row will be updated.
     * 
     * @param context The current DSpace context.
     * @param uuid The item uuid to put in for enhancement.
     * @param entityType The entity type of the item to add for enhancement.
     */
    @Override
    public void addItemForEnhancement(Context context, UUID uuid, String entityType) {
        logger.info("Adding item to enhancement table: " + uuid);
        try {
            uclouvainItemEnhancerDAO.addOrUpdateItemToUpdate(context, uuid, entityType);
        } catch (Exception e) {
            logger.error("An error occurred while posting item to the database for enhancement.", e);
        }
    }

    /**
     * Retrieve all the entries in the 'uclouvain_item_authority_metadata_enhancement' database table.
     * Can return an empty list if nothing found or if an error occurred.
     * 
     * @param context The current DSpace context.
     * @return A list of {@link ItemToEnhance} classes which might be empty.
     */
    @Override
    public List<ItemToEnhance> getItemsToEnhance(Context context) {
        return getItemsToEnhance(context, pullLimit);
    }

    /**
     * Retrieve all the entries in the 'uclouvain_item_authority_metadata_enhancement' database table.
     * Can return an empty list if nothing found or if an error occurred.
     * 
     * @param context The current DSpace context.
     * @param limit The limit amount of object that can be returned.
     * @return A list of {@link ItemToEnhance} classes which might be empty.
     */
    @Override
    public List<ItemToEnhance> getItemsToEnhance(Context context, Integer limit) {
        try {
            // Call the DAO to get all entries from the database table.
            return uclouvainItemEnhancerDAO.getItemsToEnhance(context, limit);
        } catch (Exception e) {
            logger.error(
                "An error occurred while retrieving the ItemToEnhance entries from the database DAO.", e
            );
            return new ArrayList<>();
        }
    }

    /**
     * Returns the total number of items that needs to be enhanced.
     * 
     * @param context The current DSpace context.
     * @return The amount of items that need to be enhanced as an int.
     */
    @Override
    public Integer countItemsToEnhance(Context context) {
        try {
            return uclouvainItemEnhancerDAO.countItemsToEnhance(context);
        } catch (SQLException e) {
            logger.error(
                "An error occurred while retrieving the number of ItemToEnhance entries from the database DAO.", e
            );
            return -1;
        }
    }

    /**
     * Clean ALL the entries related to an item.
     * If the item uuid is referenced as a source or a target in a row of the table, the row is deleted.
     * 
     * @param context The current DSpace context.
     * @param uuid The uuid of the item to delete entries for.
     * @return An integer giving the number of rows that were deleted.
     */
    @Override
    public Integer cleanForItem(Context context, UUID uuid) {
        try {
            System.out.println("Cleaning all items to enhance...");
            return uclouvainItemEnhancerDAO.cleanTableEntriesForItem(context, uuid);
        } catch (Exception e) {
            logger.error("An error occurred while cleaning entries for specific UUID: " + uuid + " exception: " + e);
            return -1;
        }
    }

    /**
     * Clean all the scheduled metadata enhancement for a given time period.
     * 
     * @param context The current DSpace context.
     * @param startDate The start of the selected period to delete.
     * @param endDate The end of the selected period to delete.
     * @return The number of cleaned metadata enhancement entries. Set -1 if any error occurred.
     */
    @Override
    public Integer cleanForDateRange(Context context, Date startDate, Date endDate) {
        try {
            Integer deletedEntries = uclouvainItemEnhancerDAO.cleanTableEntries(context, startDate, endDate);
            logger.debug(
                "Deleted " + deletedEntries + " entries from the database from date "
                + startDate + " to " + endDate
            );
            return deletedEntries;
        } catch (Exception e) {
            logger.error(
                "An error occurred while trying to clean entries for the specified date range = "
                + "'startDate:' " + startDate + " 'endDate': " + endDate
            );
            return -1;
        }
    }

    /**
     * Find any item linked to a given uuid on the specified metadata field.
     * 
     * @param context The current DSpace application context.
     * @param metadataField The metadata field that has to be linked to the provided uuid.
     * @param uuid The authority value to search for.
     * @return Any item that has a link to the given authority on the given metadata field.
     */
    public List<Pair<Item, Integer>> getAuthorityLinkedItems(Context context, MetadataField metadataField, UUID uuid) {
        try {
            return uclouvainItemEnhancerDAO.getAuthorityLinkedItem(context, metadataField, uuid.toString());
        } catch (SQLException e) {
            logger.error(
                "Could not find authority linked items for given authority %s and metadataField %s".formatted(
                    uuid.toString(), metadataField.toString('.')
                )
            );
            return Collections.emptyList();
        }
    }

    /**
     * Retrieve a list of supported enhancers for a given entity-type and action.
     */
    public List<MetadataEnhancer<Object>> getEnhancers(String entityType, String action) {
        Map<String, List<MetadataEnhancer<Object>>> actionMap
            = enhancersMap.getOrDefault(entityType, Collections.emptyMap());
        return actionMap.getOrDefault(action, Collections.emptyList());
    }

    /**
     * Retrieve a list of supported enhancers for a given entity-type and actions.
     */
    public List<MetadataEnhancer<Object>> getEnhancers(String entityEntity, List<String> actions) {
        return actions.stream()
            .map(action -> getEnhancers(entityEntity, action))
            .flatMap(list -> list.stream())
            .distinct()
            .toList();
    }

    // GETTERS && SETTERS
    public void setEnhancers(List<MetadataEnhancer<Object>> enhancers) {
        this.enhancers = enhancers;
    }

    public List<MetadataEnhancer<Object>> getEnhancers() {
        return enhancers;
    }

    public void setPullLimit(Integer limit) {
        pullLimit = limit;
    }

    public Integer getPullLimit() {
        return pullLimit;
    }
}
