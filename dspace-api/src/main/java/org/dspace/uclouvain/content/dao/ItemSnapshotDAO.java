/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.uclouvain.content.snapshot.ItemSnapshot;

/**
 * Database Access Object interface class for the {@link ItemSnapshot} object.
 * The implementation of this class is responsible for all database calls for the ItemSnapshot object and is autowired
 * by spring framework.
 *
 * !!!This class should only be accessed from a single service and should never be exposed outside the API
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
public interface ItemSnapshotDAO extends GenericDAO<ItemSnapshot> {

    /**
     * Search about {@link ItemSnapshot} related to an {@link org.dspace.content.Item}
     *
     * @param context the application context.
     * @param item the item for which to search the snapshot.
     * @return the snapshot stored into database corresponding to the item.
     * @throws SQLException if any database errors occurred.
     */
    ItemSnapshot findByItem(Context context, Item item) throws SQLException;

    /**
     * Search about items UUID that need to be snapshotted.
     * An item is eligible when it is archived and either has no stored snapshot yet,
     * or has been modified after the timestamp of its own stored snapshot.
     *
     * @param context the application context
     * @param from an optional additional lower boundary; when specified, only items modified after this timestamp are
     *             returned.
     *             When null, every item whose snapshot is missing or outdated is eligible.
     * @param limit the maximum number of item to return (use -1 to unlimited)
     * @return the list of item UUID to should be snapshotted and updated into the database
     * @throws SQLException if any database errors occurred.
     */
    List<UUID> findItemsToSnapshot(Context context, Date from, int limit) throws SQLException;
    default List<UUID> findItemsToSnapshot(Context context, Date from) throws SQLException {
        return this.findItemsToSnapshot(context, from, -1);
    }
    default List<UUID> findItemsToSnapshot(Context context) throws SQLException {
        return this.findItemsToSnapshot(context, null, -1);
    }


}
