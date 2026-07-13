/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.tasks;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.content.snapshot.ItemSnapshot;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.core.NotificationType;
import org.dspace.uclouvain.core.mails.NotificationTarget;
import org.dspace.uclouvain.core.mails.Recipient;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.ItemSnapshotService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class SnapshotDetectingChangesTask {

    private final Logger logger = LogManager.getLogger(SnapshotDetectingChangesTask.class);
    private final ItemSnapshotService snapshotService = UCLouvainServiceFactory.getInstance().getSnapshotService();
    private final ConfigurationService configService = DSpaceServicesFactory
        .getInstance()
        .getConfigurationService();
    private final boolean taskEnable = configService.getBooleanProperty("item-snapshot.scheduled-task.enabled", false);
    private final int itemsLimit = configService.getIntProperty("item-snapshot.scheduled-task.items-limit", -1);
    private NotificationType notifyBy;

    @PostConstruct
    public void init() {
        logger.info("Initializing SnapshotDetectingChangesTask");
        logger.info("\tTask enabled state: {}", taskEnable);
        logger.info("\tCron scheduling initialized with expression: {}",
            configService.getProperty("item-snapshot.scheduled-task.cron"));
        logger.info("\tRetrieve item snapshot limit: {}", itemsLimit);

        String notifyByConfig = configService.getProperty("item-snapshot.scheduled-task.notify-by");
        if (StringUtils.isNotBlank(notifyByConfig)) {
            try {
                this.notifyBy = NotificationType.valueOf(notifyByConfig);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid notify-by config property: {}", notifyByConfig);
            }
        }

    }

    @Scheduled(cron = "${item-snapshot.scheduled-task.cron}")
    public void triggerCron() {
        if (taskEnable) {
            logger.info("Starting snapshot detection task...");
            run();
        }
    }

    /** The main method to call when the task is performed by cron job */
    private void run() {
        try (Context context = new Context()) {
            context.turnOffAuthorisationSystem();
            List<UUID> itemIDs = snapshotService.findItemsToSnapshot(context, null, itemsLimit);
            performItems(context, itemIDs, notifyBy);
            context.complete();
            logger.debug("Entering sleeping mode ... ZZZzz");
        } catch (SQLException e) {
            logger.error("An error occurred during the snapshot process execution", e);
        }
    }

    /**
     * Detecting changes (and possibly notify them) for a list of item
     * @param context DSpace application context
     * @param itemIDs list of item UUID to analyze
     * @param notifyBy notification method to use if some diff are detected. See {@link Recipient} for possible values
     */
    public void performItems(Context context, List<UUID> itemIDs, NotificationType notifyBy) {
        if (itemIDs.isEmpty()) {
            logger.info("No items to snapshot detected");
            return;
        }
        logger.info("Performing snapshot detection task on {} item(s)", itemIDs.size());
        // DEV NOTES :: Why using `NotificationTarget` instead of `Recipient` ?
        //   We will ensure to send only one communication (email, SMS, ...) by recipient box.
        //   But for a publication, multiple authors could share same email but not same name (ex: grouped mailbox)
        //   Using `NotificationTarget` (equals and hashCode), we will ensure this fact
        Map<NotificationTarget, List<ItemSnapshotDiff>> dataToNotify = new HashMap<>();
        boolean shouldNotify = notifyBy != null;
        for (UUID itemID : itemIDs) {
            try {
                ItemSnapshot snapshot = snapshotService.takeSnapshot(context, itemID);
                ItemSnapshot storedSnapshot = snapshotService.get(context, itemID);
                // If no snapshot is stored into database for the item, the only thing to do is to preserve the
                // newly fresh snapshot into database; no need to detect changes and/or notify any recipients
                if (storedSnapshot == null) {
                    logger.info("\tNewly fresh snapshot for item#{} detected.", itemID);
                    snapshotService.store(context, snapshot);
                    context.commit();
                    continue;
                }
                // If a snapshot already exists into database for this item, try to detect any changes.
                // If some changes are detected, we would (maybe) notify recipients related to this item.
                // In all cases, the snapshot must be updated into database
                ItemSnapshotDiff diff = snapshotService.compareSnapshot(storedSnapshot, snapshot);
                if (diff == null) {
                    logger.warn("\tUnable to get snapshotDiff for item#{} ! --> NULL.", itemID);
                    continue;
                }
                // Always store the updated snapshot to update timestamps, even if no changes are detected
                snapshotService.store(context, snapshot);
                if (!diff.hasChanges()) {
                    logger.info("\tNo snapshot changes detected for item#{}.", itemID);
                    continue;
                }
                logger.info("\t{} snapshot changes detected for item#{}", diff.getChanges().size(), itemID);
                if (shouldNotify) {
                    snapshotService.getNotifyRecipients(context, diff, notifyBy).stream()
                        .filter(recipient -> StringUtils.isNotBlank(recipient.get(notifyBy)))
                        .forEach(recipient -> {
                            logger.debug("\t\t* notify change to \"{}\".", recipient.get(notifyBy));
                            NotificationTarget target = new NotificationTarget(recipient, notifyBy);
                            dataToNotify.computeIfAbsent(target, k -> new ArrayList<>()).add(diff);
                        });
                }
                context.commit();
            } catch (Exception e) {
                logger.error("Unable to perform snapshot comparison for item#%s".formatted(itemID), e);
            }
        }
        // If we detect some changes and these changes should be notified, delegate notification process to
        // dedicated service.
        if (shouldNotify && !dataToNotify.isEmpty()) {
            logger.info("\tNotifying {} recipients for changes ::", dataToNotify.size());
            for (Map.Entry<NotificationTarget, List<ItemSnapshotDiff>> entry : dataToNotify.entrySet()) {
                String contact = entry.getKey().recipient().get(notifyBy);
                try {
                    logger.info("\t\t* Notify {} for {} changes.", contact, entry.getValue().size());
                    snapshotService.notifyRecipient(entry.getKey().recipient(), entry.getValue(), notifyBy);
                } catch (Exception e) {
                    logger.error("\tUnable to notify recipient {} for changes ::", contact, e);
                }
            }
        }
    }


}
