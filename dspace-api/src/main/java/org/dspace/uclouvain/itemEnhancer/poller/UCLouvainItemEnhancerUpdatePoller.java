/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.poller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.dspace.uclouvain.itemEnhancer.enhancers.ItemEnhancerConfiguration;
import org.dspace.uclouvain.itemEnhancer.exceptions.WrongEntityTypeException;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Poller to process desired items and update their metadata values.
 * This class reads the 'uclouvain_item_authority_metadata_enhancement' table and process all its entries.
 * Each time entries are pulled, the table is cleaned, and later refilled with other entries.
 * One entry represent a link between two items: a 'target' and a 'source'.
 * The main goal of this poller is to update the target item using the source item metadata
 * and a specific configuration.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@Component
@EnableScheduling
public class UCLouvainItemEnhancerUpdatePoller {

    @Autowired
    UCLouvainItemEnhancerService itemEnhancerService;

    @Autowired
    ItemService itemService;

    private Logger logger = LogManager.getLogger(UCLouvainItemEnhancerUpdatePoller.class);
    private boolean isScheduledEnabled = DSpaceServicesFactory
        .getInstance()
        .getConfigurationService()
        .getBooleanProperty("uclouvain-item-enhancer-poller.enabled", false);

    /**
     * Main poller task, executed every x seconds.
     * NOTE: use 'fixedDelay' instead of 'fixedRate' since 'fixedDelay' waits for the execution to end
     * before triggering countdown.
     */
    @Scheduled(fixedDelayString = "${uclouvain-item-enhancer-poller.delay}")
    public void triggerCycleCheck() {
        if (isScheduledEnabled) {
            run();
        }
    }

    /**
     * Main method to trigger the main poller functionality.
     * This method is called through triggerCycleCheck() with a configured fixed delay.
     * For tests this method is called directly, this is why it is public.
     */
    public void run() {
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        try {
            // Retrieve items to update form the database table.
            List<ItemToEnhance> itemsToUpdate = itemEnhancerService.getItemsToEnhance(context);
            if (itemsToUpdate.isEmpty()) {
                context.complete();
                return;
            }
            logger.debug("Poller found " + itemsToUpdate.size() + " items to update in the database !");

            // Get max/min dates form list of ItemToEnhance.
            Date maxDate = itemsToUpdate.stream().map(x -> x.getDateQueued()).max(Date::compareTo).get();
            Date minDate = itemsToUpdate.stream().map(x -> x.getDateQueued()).min(Date::compareTo).get();

            // Generate a map of <TargetItem, List<SourceItem>> to use later.
            HashMap<Item, List<Item>> map = generateHashMap(itemsToUpdate);

            // Clean table from all entries between the max && min dates because they are being processed.
            Integer deletedEntries = itemEnhancerService.cleanForDateRange(context, minDate, maxDate);

            if (deletedEntries != itemsToUpdate.size()) {
                logger.warn(
                    "The number of deleted entries and processed items do not match."
                    + " Dates used in delete query = 'minDate': " + minDate + " 'maxDate': " + maxDate + "."
                    + " Unprocessed entries might have been removed from the database."
                );
            }

            // Update target items if needed.
            processUpdates(context, map);
            context.complete();
        } catch (Exception e) {
            logger.error("An error happened in the item enhancer poller: " + Arrays.asList(e.getStackTrace()));
            context.abort();
        }
        context.restoreAuthSystemState();
    }

    /**
     * Generates a HashMap from a list of 'ItemToEnhance' items.
     * The keys are target Items and the values are list of source items.
     * 
     * @param itemsToUpdate List of source/target items pair retrieved from the database.
     * @return The generated HashMap, might be empty.
     */
    private HashMap<Item, List<Item>> generateHashMap(List<ItemToEnhance> itemsToUpdate) {
        HashMap<Item, List<Item>> hashMap = new HashMap<Item, List<Item>>();
        for (ItemToEnhance ite: itemsToUpdate) {
            Item source = ite.getSourceItem();
            Item target = ite.getTargetItem();
            if (source == null || target == null) {
                logger.warn(
                    "An item could not be found from a itemToEnhance entry: source=" + source + " target=" + target
                );
                continue;
            }

            // This expression allows to add an item to an already existing key
            // or to create the key and set an empty list to it.
            hashMap.computeIfAbsent(target, key -> new ArrayList<Item>()).add(source);
        }
        return hashMap;
    }

    /**
     * Process the necessary updates using the provided item map.
     * Update if the configured metadata fields value differs from the source.
     * 
     * @param context The current DSpace context.
     * @param itemMap The map of item to use to make the right updates.
     */
    private void processUpdates(Context context, HashMap<Item, List<Item>> itemMap) throws SQLException {
        // Loop over each target item.
        for (Item target: itemMap.keySet()) {
            try {
                boolean isTargetItemChanged = false;
                // For each target, get the corresponding sources.
                for (Item source: itemMap.get(target)) {
                    // Then retrieve the corresponding configuration for the current source && target.
                    for (ItemEnhancerConfiguration config: retrieveConfiguration(source, target)) {
                        // For each configuration, get the value for the source item, then update if required.
                        String relevantValue = getFirstValidSourceMetadataValue(context, source, config);
                        if (relevantValue != null) {
                            // For each target metadata value, update the corresponding values if required.
                            for (String targetMdField: config.getTargetMetadataFields()) {
                                try {
                                    boolean metadataChanged = updateItemMetadata(
                                        context, source, target, relevantValue, targetMdField
                                    );
                                    isTargetItemChanged = isTargetItemChanged || metadataChanged;
                                } catch (Exception e) {
                                    logger.error(
                                        "Could not update item metadata with source value for item " + target.getID()
                                    );
                                }
                            }
                        }
                    }
                }
                if (isTargetItemChanged) {
                    // If the target item has changed, we must update it.
                    itemService.update(context, target);
                }
            } catch (Exception e) {
                logger.error("Could not update target item metadata for item: " + target.getID());
            }
        }
        context.commit();
    }

    /**
     * Get the corresponding configurations for given source and target items.
     * @param source Source item.
     * @param target Target item.
     * @return List of valid configurations for the given source and target items.
     */
    private List<ItemEnhancerConfiguration> retrieveConfiguration(Item source, Item target) {
        try {
            return itemEnhancerService.getValidConfigurationsForSourceAndTarget(source, target);
        } catch (WrongEntityTypeException e) {
            logger.error(
                "An error occurred when retrieving valid configuration in poller for source item: "
                + source.getID() + " and target item: " + target.getID(), e
            );
            return new ArrayList<ItemEnhancerConfiguration>();
        }
    }

    /**
     * Little method to return the first valid metadata for a source item.
     * @param context The current DSpace context.
     * @param sourceItem The item to extract metadata from.
     * @param config The enhancement configuration.
     * @return The first valid configured metadata value found in the source item. May return null.
     */
    private String getFirstValidSourceMetadataValue(
        Context context, Item sourceItem, ItemEnhancerConfiguration config
    ) {
        for (String mdField: config.getSourceMetadataFields()) {
            String mv = itemService.getMetadataFirstValue(sourceItem, new MetadataFieldName(mdField), Item.ANY);
            if (mv != null) {
                return mv;
            }
        }
        return null;
    }

    /**
     * Update the target's metadata if necessary according to the value coming from the source item.
     * @param context The current DSpace Context.
     * @param sourceItem The item holding the reference to the correct metadata value.
     * @param targetItem The item to potentially update.
     * @param newValue The value to use for the update.
     * @return Boolean true if the target item's metadata was modified. If no modifications, return false.
     * @throws SQLException
     * @throws AuthorizeException
     */
    private boolean updateItemMetadata(
        Context context, Item sourceItem, Item targetItem, String newValue, String fieldToCheck
    ) throws SQLException, AuthorizeException {
        List<MetadataValue> mdToRemove = new ArrayList<MetadataValue>();
        for (MetadataValue mdValue:
            itemService.getMetadata(targetItem, fieldToCheck, sourceItem.getID().toString())) {
            if (!mdValue.getValue().equals(newValue)) {
                mdToRemove.add(mdValue);
            }
        }
        if (mdToRemove.isEmpty()) {
            return false;
        }
        itemService.removeMetadataValues(context, targetItem, mdToRemove);
        for (MetadataValue removedMd: mdToRemove) {
            logger.info(
                "Updating metadata value for field: " + removedMd.getMetadataField()
                + " of target item: " + targetItem.getID()
            );
            itemService.addMetadata(
                context,
                targetItem,
                removedMd.getSchema(), removedMd.getElement(), removedMd.getQualifier(),
                removedMd.getLanguage(),
                newValue,
                removedMd.getAuthority(), removedMd.getConfidence(), removedMd.getPlace()
            );
        }
        return true;
    }
}
