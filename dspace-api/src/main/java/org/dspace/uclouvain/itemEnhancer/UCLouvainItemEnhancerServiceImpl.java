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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.uclouvain.itemEnhancer.dao.UCLouvainItemEnhancerDAO;
import org.dspace.uclouvain.itemEnhancer.enhancers.ItemEnhancerConfiguration;
import org.dspace.uclouvain.itemEnhancer.exceptions.WrongEntityTypeException;
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

    // The full list of metadata enhancers configuration. See 'uclouvain-metadata-enhancers.xml' for full config.
    private List<ItemEnhancerConfiguration> enhancers = new ArrayList<ItemEnhancerConfiguration>();
    private Logger logger = LogManager.getLogger(UCLouvainItemEnhancerServiceImpl.class);

    // Defines the maximum number of items to retrieve at the same time from the database using
    // the 'getItemsToEnhance' method.
    private Integer pullLimit = 1000;

    @Autowired
    private ItemService itemService;

    @Autowired
    private MetadataFieldService metadataFieldService;

    @Autowired
    private UCLouvainItemEnhancerDAO uclouvainItemEnhancerDAO;

    /**
     * Get all the applicable configurations for a specific source item.
     * If the source item has the same entity type as the config and holds a value for at least one
     * of the configured metadata fields, then it is applicable.
     * Returns an empty list if no configuration is matching.
     * 
     * @param item The source item to use in order to find applicable configurations.
     * @return A list of applicable configurations, which could be empty if none found.
     */
    @Override
    public List<ItemEnhancerConfiguration> getValidConfigurationsForItem(Item item) {
        // First get entity type of the item.
        String itemEntityType = itemService.getEntityType(item);
        List<ItemEnhancerConfiguration> validConfigurations = new ArrayList<ItemEnhancerConfiguration>();
        if (itemEntityType == null) {
            logger.warn(
                "Cannot get valid configuration for item " + item.getID() + " because the EntityType cannot be found."
            );
            return validConfigurations;
        }

        // Loop over all the configurations and find valid ones using the item entity type.
        enhancers
            .stream()
            .filter(enhancer -> enhancer.getSourceEntityType().equals(itemEntityType))
            .forEach(enhancer -> {
                for (String mdField: enhancer.getSourceMetadataFields()) {
                    if (itemService.getMetadata(item, mdField, Item.ANY) != null) {
                        // If at least one of the configured source metadata field is present in the item,
                        // add it to valid config.
                        validConfigurations.add(enhancer);
                        break;
                    }
                }
            });
        logger.debug("Recovered list of config for item " + item.getID() + ": " + validConfigurations);
        return validConfigurations;
    }

    /**
     * Get all the applicable configurations for specific source && target item.
     * To be considered applicable the following conditions must be met:
     * - The source item has the same entity type as provided by the configuration,
     * - The target item has the same entity type as provided by the configuration.
     * Returns an empty list if no configuration is matching.
     * 
     * @param source The source item to use in order to find applicable configurations.
     * @param target The target item to use in order to find applicable configurations.
     * @return A list of applicable configurations, which could be empty if none found.
     */
    @Override
    public List<ItemEnhancerConfiguration> getValidConfigurationsForSourceAndTarget(Item source, Item target)
        throws WrongEntityTypeException {
        String sourceEntityType = this.itemService.getEntityType(source);
        if (sourceEntityType == null) {
            throw new WrongEntityTypeException("No entity type found for given source item " + source.getID());
        }
        String targetEntityType = this.itemService.getEntityType(target);
        if (targetEntityType == null) {
            throw new WrongEntityTypeException("No entity type found for given target item " + target.getID());
        }

        // Loop over all the configurations and find valid ones.
        List<ItemEnhancerConfiguration> validConfigurations =
            this.enhancers.stream()
            .filter(enhancer -> enhancer.isValidForEntityTypes(sourceEntityType, targetEntityType))
            .collect(Collectors.toList());

        return validConfigurations;
    }

    /**
     * Given a list of applicable configurations and a source item, this method will add an entry
     * into the database for each related target item.
     * Target items are found by searching for items holding the corresponding authority on the
     * configured metadata fields.
     * 
     * A target item is considered valid if its metadata is different from the source it is coming from.
     * If a target item is valid, it is added in the database table 'uclouvain_item_authority_metadata_enhancement'
     * to be processed later by a 'poller'.
     * 
     * @param context The current DSpace context.
     * @param sourceItem The source item to get the correct value from.
     * @param validConfigurations A list of all the valid configuration used to search for valid target item.
     */
    @Override
    public void addRelatedItemsForEnhancement(
        Context context, Item sourceItem, List<ItemEnhancerConfiguration> validConfigurations
    ) {
        UUID sourceUUID = sourceItem.getID();
        logger.debug("Found valid item to process: " + sourceItem.getID());

        for (ItemEnhancerConfiguration config : validConfigurations) {
            try {
                for (String mdField: config.getTargetMetadataFields()) {
                    // !! Warning: Retrieving each time the metadata field could be damaging for
                    // !! performance. It triggers a database call.
                    MetadataField field = metadataFieldService.findByString(context, mdField, '.');
                    // If field is unknown, skip it.
                    if (field == null) {
                        logger.warn("Could not find valid MetadataField object for configured field '" + mdField + "'");
                        continue;
                    }

                    // Find all linked items that have a metadata with the right authority.
                    logger.debug("Searching target item for source item " + sourceUUID);
                    List<Item> linkedItems =
                        uclouvainItemEnhancerDAO.getAuthorityLinkedItem(context, field, sourceUUID.toString());
                    for (Item linkedItem: linkedItems) {
                        if (isTargetItemValid(context, linkedItem, sourceItem, config, mdField)) {
                            // Let's use the DAO to add entry to the database
                            logger.info("Found valid linked target item: "
                                + linkedItem.getID() + ", inserting in DB...");
                            uclouvainItemEnhancerDAO.addOrUpdateItemToUpdate(
                                context, sourceUUID, linkedItem.getID()
                            );
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("An error occurred while posting related target item to the database.", e);
            }
        }
    }

    /**
     * Check if a target item should be added to the queue by checking its validity
     * and the state of its metadata values.
     * 
     * @param context The current DSpace context.
     * @param targetItem The target item to check the validity of.
     * @param sourceItem The source item which is holding the source value.
     * @param config The current enhancement configuration we are processing.
     * @param targetField The current field for which we want to check the value.
     * @return True if the target item has the right entityType and has an outdated value for a field. False otherwise.
     */
    private boolean isTargetItemValid(
        Context context, Item targetItem, Item sourceItem, ItemEnhancerConfiguration config, String targetField
    ) {
        String targetEntityType = itemService.getEntityType(targetItem);
        if (!config.getTargetEntityType().equals(targetEntityType)) {
            logger.debug(
                "Wrong entityType for target item, wanted '"
                + config.getTargetEntityType() + "' found '"
                + targetEntityType + "'"
            );
            return false;
        }

        List<String> targetValues = itemService
            .getMetadata(targetItem, targetField, sourceItem.getID().toString())
            .stream().map(x -> x.getValue()).collect(Collectors.toList());
        if (targetValues == null) {
            return false;
        }

        logger.debug("TargetValues found for field " + targetField + ": " + targetValues);

        // Flag to determine if the target item has different values compared to the source.
        boolean didMetadataChange = false;

        for (String sourceMdField: config.getSourceMetadataFields()) {
            String sourceValue =
                itemService.getMetadataFirstValue(sourceItem, new MetadataFieldName(sourceMdField), Item.ANY);
            logger.debug("Source value to compare: " + sourceValue);
            if (sourceValue == null) {
                continue;
            }
            // Browse each value for the target metadata field.
            // If one value does not match the source, toggle the 'isMetadataChanged' flag.
            for (String targetValue: targetValues) {
                logger.debug("Checking value changes, source = " + sourceValue + ", target = " + targetValue);
                if (!targetValue.equals(sourceValue)) {
                    didMetadataChange = true;
                    break;
                }
            }
            // If we found any valid metadata field in the source we can exit the loop.
            break;
        }
        logger.debug(
            "Metadata " + (didMetadataChange ? "changed" : "did not change")
            + " for item " + targetItem.getID() + " with field " + targetField
        );
        return didMetadataChange;
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


    // GETTERS && SETTERS
    public void setEnhancers(List<ItemEnhancerConfiguration> enhancers) {
        this.enhancers = enhancers;
    }

    public List<ItemEnhancerConfiguration> getEnhancers() {
        return enhancers;
    }

    public void setPullLimit(Integer limit) {
        pullLimit = limit;
    }

    public Integer getPullLimit() {
        return pullLimit;
    }
}
