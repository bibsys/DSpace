/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.poller;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.mails.EnhancementErrorNotifyEmail;
import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.dspace.uclouvain.itemEnhancer.enhancers.MetadataEnhancer;
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
 * The main goal of this poller is to update the target item using the source item metadata.
 * To do that we call a set of 'enhancers'.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@Component
@EnableScheduling
public class UCLouvainItemEnhancerPoller {

    @Autowired
    UCLouvainItemEnhancerService itemEnhancerService;

    @Autowired
    ItemService itemService;

    private Logger logger = LogManager.getLogger(UCLouvainItemEnhancerPoller.class);
    private ConfigurationService configService = DSpaceServicesFactory
        .getInstance()
        .getConfigurationService();
    private boolean isScheduledEnabled = configService.getBooleanProperty(
        "uclouvain-item-enhancer-poller.enabled", false);
    private int pullLimit = configService.getIntProperty(
        "uclouvain-item-enhancer-poller.limit", 100);
    private boolean enableErrorNotify = configService.getBooleanProperty(
        "uclouvain.enhancement_error_notify.mail.enabled", false
    );

    /**
     * Main poller task, executed every x seconds.
     * DEV_NOTE: use 'fixedDelay' instead of 'fixedRate' because 'fixedDelay' waits for the execution to end
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

        // Retrieve items to update form the database table.
        List<ItemToEnhance> itemsToEnhance = itemEnhancerService.getItemsToEnhance(context, pullLimit);
        if (itemsToEnhance.isEmpty()) {
            // Early exit if nothing to enhance.
            context.restoreAuthSystemState();
            context.close();
            return;
        }
        logger.info("Poller found " + itemsToEnhance.size() + " items to update in the database !");

        // Get max/min dates form list of ItemToEnhance.
        Date maxDate = itemsToEnhance.stream().map(x -> x.getDateQueued()).max(Date::compareTo).get();
        Date minDate = itemsToEnhance.stream().map(x -> x.getDateQueued()).min(Date::compareTo).get();

        for (ItemToEnhance enhancement : itemsToEnhance) {
            try {
                boolean requiresUpdate = enhance(
                    context,
                    enhancement.getItemUUID(),
                    enhancement.getEntityType()
                );
                if (requiresUpdate) {
                    context.commit();
                }
            } catch (Exception e) {
                notifyError(context, enhancement.getItemUUID(), enhancement.getEntityType(), e);
                // Always log the error, even if the notification is disabled.
                logger.error("Could not enhance item %s with entityType %s".formatted(
                    enhancement.getItemUUID().toString(), enhancement.getEntityType()), e);
                try {
                    context.rollback();
                } catch (SQLException rollbackException) {
                    logger.error("Could not rollback context following enhancement error.", rollbackException);
                    // Close the context and open up a new one to resolve the rollback issues.
                    context.close();
                    context = new Context();
                }
            }
        }

        // Clean table from all entries between the max && min dates because they are being processed.
        Integer deletedEntries = itemEnhancerService.cleanForDateRange(context, minDate, maxDate);
        if (deletedEntries > itemsToEnhance.size()) {
            logger.warn(
                "The number of deleted entries and processed items do not match."
                + " Dates used in delete query = 'minDate': " + minDate + " 'maxDate': " + maxDate + "."
                + " Unprocessed entries might have been removed from the database."
            );
        }

        try {
            context.complete();
        } catch (Exception e) {
            logger.error("Could not complete context", e);
            context.abort();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private boolean enhance(Context context, UUID uuid, String entityType) throws Exception {
        boolean updated = false;
        Item item = itemService.find(context, uuid);
        if (item == null) {
            // Consider this as a DELETE operation.
            List<MetadataEnhancer<Object>> enhancers =
                itemEnhancerService.getEnhancers(entityType, MetadataEnhancer.ACTION_DELETE);
            for (MetadataEnhancer<Object> enhancer : enhancers) {
                updated = enhancer.enhance(context, uuid) || updated;
            }
        } else {
            // Consider this as an UPDATE or CREATE operation.
            List<MetadataEnhancer<Object>> enhancers = itemEnhancerService.getEnhancers(
                entityType,
                Arrays.asList(MetadataEnhancer.ACTION_CREATE, MetadataEnhancer.ACTION_UPDATE)
            );
            for (MetadataEnhancer<Object> enhancer : enhancers) {
                updated = enhancer.enhance(context, item) || updated;
            }
        }
        return updated;
    }

    /**
     * Notify admins of the error.
     * 
     * @param context The current DSpace context.
     * @param uuid The uuid of the item for which we got an error.
     * @param entityType The entity type of the item fo which we got an error.
     * @param error The error.
     */
    private void notifyError(Context context, UUID uuid, String entityType, Exception error) {
        // If configuration is enabled, send an error email to admins to keep track of the error.
        if (enableErrorNotify) {
            try {
                new EnhancementErrorNotifyEmail(context, uuid, entityType, error).sendEmail();
            } catch (Exception emailException) {
                logger.warn(
                    "Could not send error email to warn of enhancement failure for uuid " + uuid, emailException);
            }
        }

    }
}
