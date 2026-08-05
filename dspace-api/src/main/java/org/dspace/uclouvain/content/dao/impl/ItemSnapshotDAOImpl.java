/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.dao.impl;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.TypedQuery;
import org.dspace.content.Item;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.dao.ItemSnapshotDAO;
import org.dspace.uclouvain.content.snapshot.ItemSnapshot;

public class ItemSnapshotDAOImpl extends AbstractHibernateDAO<ItemSnapshot> implements ItemSnapshotDAO {

    /**
     * Search about {@link ItemSnapshot} related to an {@link Item}
     *
     * @param context the application context.
     * @param item the item for which to search the snapshot.
     * @return the snapshot stored into database corresponding to the item. null if no snapshot could be found.
     * @throws SQLException if any database errors occurred.
     */
    @Override
    public ItemSnapshot findByItem(Context context, Item item) throws SQLException {
        return this.findByID(context, ItemSnapshot.class, item.getID());
    }

    /**
     * Search about items UUID that need to be snapshotted.
     * DEV NOTE :: The staleness of an item MUST be evaluated against **its own** snapshot timestamp, never against a
     *             global boundary shared by all items. Using a global boundary (for example the most recent timestamp
     *             found into the `uclouvain_item_snapshot` table) silently hides every item modified before that
     *             boundary but after its own snapshot, and those changes would never be detected again.
     *
     * @param context the application context
     * @param from an optional additional lower boundary; when specified, only items modified after this timestamp are
     *             returned. When null, every item whose snapshot is missing or outdated is eligible.
     * @param limit the maximum number of item to return (use -1 to unlimited)
     * @return the list of item UUID to should be snapshotted and updated into the database
     * @throws SQLException if any database errors occurred.
     */
    @Override
    public List<UUID> findItemsToSnapshot(Context context, Date from, int limit) throws SQLException {
        String queryString = """
            SELECT i.id FROM Item i
            LEFT JOIN ItemSnapshot s ON i.id = s.id
            WHERE i.inArchive = true
              AND (s.id IS NULL OR i.lastModified > s.timestamp)
            """
            + ((from != null) ? "  AND i.lastModified > :fromDate\n" : "")
            + "ORDER BY i.lastModified ASC";
        TypedQuery<UUID> query = getHibernateSession(context).createQuery(queryString, UUID.class);
        if (from != null) {
            query.setParameter("fromDate", from);
        }
        if (limit != -1) {
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }
}
