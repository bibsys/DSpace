/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import java.sql.SQLException;
import java.util.HashSet;
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
import org.dspace.uclouvain.content.cleanMetadata.CleanMetadataService;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;

/**
 * Consumer to clean item metadata from undesired fields
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class FormFieldCleanerConsumer implements Consumer {

    private final Logger logger = LogManager.getLogger(FormFieldCleanerConsumer.class);

    private ItemService itemService;
    private CleanMetadataService cleanMetadataService;
    private Set<UUID> itemToProcess;

    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
        cleanMetadataService = UCLouvainServiceFactory.getInstance().getCleanMetadataService();
        itemToProcess = new HashSet<>();
    }

    /**
     * Check if the item of the event is valid.
     * An item is valid if it has a value for the 'typeBindField'.
     * When an item is valid, we add its uuid to the set of item to process.
     * 
     * @param context The current DSpace context.
     * @param event The event to process.
     */
    @Override
    public void consume(Context context, Event event) throws SQLException {
        Item item = (Item) event.getSubject(context);
        if (item == null) {
            return;
        }
        itemToProcess.add(item.getID());
    }

    /**
     * Process all valid items.
     * Get the collection of the workspace item in order to retrieve the correct submission form.
     * Build a list of all non-valid type-bind controlled field.
     * If the item has a metadata for one of those field: delete it.
     * 
     * @param context The current DSpace context.
     */
    @Override
    public void end(Context context) throws Exception {
        for (UUID itemID: itemToProcess) {
            try {
                Item item = itemService.find(context, itemID);
                cleanMetadataService.cleanMetadata(context, item);
            } catch (Exception e) {
                logger.error(
                    "An error occurred while trying to clear unwanted type-bind data of item: " + itemID.toString(), e
                );
            }
        }
        itemToProcess.clear();
    }

    @Override
    public void finish(Context context) throws Exception {}
}
