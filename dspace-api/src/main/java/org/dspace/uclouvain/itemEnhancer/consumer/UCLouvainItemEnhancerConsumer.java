/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.consumer;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.dspace.uclouvain.itemEnhancer.enhancers.ItemEnhancerConfiguration;

/**
 * Main consumer for the item enhancer functionality.
 * This consumer is triggered at events and checks the configuration to see if an item can be processed.
 * 
 * To avoid context status/version problems, we store all items uuid in a set in the 'consumer()' method.
 * Then, in the 'end()' method, we retrieve each item using the itemService and check if we can process them.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainItemEnhancerConsumer implements Consumer {

    private Set<UUID> itemsToProcess = new HashSet<>();
    private Logger logger = LogManager.getLogger(UCLouvainItemEnhancerConsumer.class);


    ItemService itemService;

    UCLouvainItemEnhancerService uclouvainItemEnhancerService;

    /**
     * Initialize the required services when the consumer is instantiated.
     */
    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
        uclouvainItemEnhancerService = UCLouvainServiceFactory.getInstance().getItemEnhancerService();
    }

    /**
     * For each event being catch, get the corresponding item and stores its uuid in the set.
     * 
     * @param context The current DSpace context.
     * @param event The fired event that may be processed by the consumer.
     */
    @Override
    public void consume(Context context, Event event) throws Exception {
        Item item = (Item) event.getSubject(context);
        if (item != null) {
            // In this case, we need to store the item uuid and not the item.
            // If we store the item, it could be an old version of the object.
            // This old version would take the place of other newer version which is not a great idea.
            // To solve this, we store the uuid and reload the item in the 'end()' method of the consumer.
            itemsToProcess.add(item.getID());
        }
    }

    /**
     * Loop over the 'itemsToProcess', check for each item if it can be processed using the service.
     * If an item can be processed, then we need to trigger a post in the table using the service.
     * 
     * @param context The current context.
     * @throws Exception
     */
    @Override
    public void end(Context context) throws Exception {
        context.turnOffAuthorisationSystem();
        // Loop over each uuid and retrieve the corresponding item.
        for (UUID uuid: itemsToProcess) {
            try {
                Item item = itemService.find(context, uuid);
                if (item == null) {
                    logger.warn("Could not retrieve item from previously stored uuid: " + uuid);
                    continue;
                }
                // Get valid configurations for the item.
                List<ItemEnhancerConfiguration> validConfigs =
                    uclouvainItemEnhancerService.getValidConfigurationsForItem(item);
                if (validConfigs != null && !validConfigs.isEmpty()) {
                    uclouvainItemEnhancerService.addRelatedItemsForEnhancement(context, item, validConfigs);
                }
            } catch (SQLException e) {
                logger.warn("An error occurred while retrieving an item via uuid in the consumer", e);
            }
        }
        context.restoreAuthSystemState();
        // Clear the set for futur use.
        itemsToProcess.clear();
    }

    @Override
    public void finish(Context ctx) throws Exception {}
}
