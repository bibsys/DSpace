/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.snapshot.ItemSnapshot;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.diff.formats.OutputFormat;
import org.dspace.uclouvain.core.NotificationType;
import org.dspace.uclouvain.core.mails.Recipient;

/**
 * Contract to respect for any classes that will deal with {@link ItemSnapshot} management
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public interface ItemSnapshotService {

    /**
     * Allow to load and get a stored {@link ItemSnapshot}
     * @param context the application context
     * @param id the item ID for which the snapshot must be found
     * @param deserialize is the snapshot content should be deserialized? Using this option we can retrieve all
     *                    snapshots into {@see ItemSnapshot#getElements()}.
     *                    Default value is `true` if not specified
     * @return the desired snapshot. Could be null if no snapshot can be loaded
     * @throws SQLException if any database exception occurred
     * @throws Exception if any other error occurred (parsing content, ...)
     */
    ItemSnapshot get(Context context, UUID id, boolean deserialize) throws Exception;
    ItemSnapshot get(Context context, UUID id) throws Exception;

    /**
     * Search about items UUID that need to be snapshotted.
     * @param context the application context
     * @param from the lower boundary timestamp limit; items updated after this timestamp could be returned if not
     *             specified, the last stored snapshot will be used
     * @param limit the maximum number of item to return (use -1 to unlimited)
     * @return the list of item UUID to should be snapshotted and updated into the database
     * @throws SQLException if any other error occurred
     */
    List<UUID> findItemsToSnapshot(Context context, Date from, int limit) throws SQLException;

    /**
     * Allow to take an instant snapshot for an item.
     * Pay attention: The snapshot doesn't yet persist into database at this point ! You need explicitly to
     *                {@see ItemSnapshotService#store()} it to persist into DB.
     * @param context the application context
     * @param id the item for which the snapshot must be done
     * @return the desired snapshot
     * @throws SQLException if any database exception occurred
     */
    ItemSnapshot takeSnapshot(Context context, UUID id) throws SQLException;
    ItemSnapshot takeSnapshot(Context context, Item item) throws SQLException;

    /**
     * Allow to found changes between two snapshot of the same item
     * @param snapshot1 the original snapshot to compare
     * @param snapshot2 the most recent snapshot to compare
     * @return the diff between both snapshot
     * @throws IllegalArgumentException if snapshots parameters are not valid
     */
    ItemSnapshotDiff compareSnapshot(ItemSnapshot snapshot1, ItemSnapshot snapshot2) throws IllegalArgumentException;

    /**
     * Allow to detect changes about an item.
     * To detect any changes, an item snapshot should already exist for the corresponding item.
     * !! Detecting changes doesn't mean that detected changes will be stored into database.
     * @param context the application context
     * @param id the item UUID to analyze
     * @return the diff changes detected from the last snapshot
     * @throws SQLException if any database exception occurred
     * @throws IllegalArgumentException if no ItemSnapshot already persisted into database for this item
     */
    ItemSnapshotDiff detectChanges(Context context, UUID id) throws Exception;
    ItemSnapshotDiff detectChanges(Context context, Item item) throws Exception;

    /**
     * This method extract as a formatted string all changes stored into a ItemSnapshotDiff
     * @param changes the diff changes detected about an item
     * @param format the rendered output format
     * @param locale the language to use to render the changes. If null, the default system language will be used
     * @return all changes formatted as desired format
     */
    String explainChanges(ItemSnapshotDiff changes, OutputFormat format, Locale locale);

    /**
     * Store and persist a snapshot into the database. If a previous snapshot already exists for the same item it will
     * be replaced.
     * @param context the application context
     * @param snapshot the snapshot to store
     * @throws SQLException if any database exception occurred
     * @throws Exception if any other error occurred (serialize, ...)
     */
    void store(Context context, ItemSnapshot snapshot) throws Exception;

    /**
     * Get all recipients to notify snapshot changes.
     * Recipients are determine by item related to a {@link ItemSnapshotDiff}
     * @param context the application context
     * @param diff the snapshot diff to analyze
     * @return the list of recipients to notify
     */
    List<Recipient> getNotifyRecipients(Context context, ItemSnapshotDiff diff, NotificationType method);

    /**
     * Notify a recipient for detected changes
     * @param context the application context
     * @param recipient the recipient to notify
     * @param changes all diff changes to notify to the recipient
     * @param method the method to notify
     */
    void notifyRecipient(Context context, Recipient recipient, List<ItemSnapshotDiff> changes, NotificationType method)
        throws Exception;
}
