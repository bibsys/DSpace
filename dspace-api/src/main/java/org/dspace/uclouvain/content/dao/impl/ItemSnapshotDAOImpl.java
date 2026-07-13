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

import jakarta.persistence.NoResultException;
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
     *
     * @param context the application context
     * @param from the lower boundary timestamp limit; items updated after this timestamp could be returned if not
     *             specified, the last stored snapshot will be used
     * @param limit the maximum number of item to return (use -1 to unlimited)
     * @return the list of item UUID to should be snapshotted and updated into the database
     * @throws SQLException if any database errors occurred.
     */
    @Override
    public List<UUID> findItemsToSnapshot(Context context, Date from, int limit) throws SQLException {
        if (from == null) {
            from = getLatestStoredSnapshotTimestamp(context);
        }
        String queryString = """
            SELECT i.id FROM Item i
            LEFT JOIN ItemSnapshot s ON i.id = s.id
            WHERE (i.lastModified > :fromDate OR s.id IS NULL)
              AND i.inArchive = true
            ORDER BY i.lastModified ASC""";
        TypedQuery<UUID> query = getHibernateSession(context).createQuery(queryString, UUID.class);
        query.setParameter("fromDate", from);
        if (limit != -1) {
            query.setMaxResults(limit);
        }
        return query.getResultList();
    }


    /**
     * Find the last stored snapshot timestamp from the database
     * @param context the application context
     * @return the last stored snapshot timestamp
     * @throws SQLException if any database errors occurred
     */
    private Date getLatestStoredSnapshotTimestamp(Context context) throws SQLException {
        try {
            Date maxTimestamp = getHibernateSession(context)
                .createQuery("SELECT MAX(i.timestamp) FROM ItemSnapshot i", Date.class)
                .getSingleResult();
            // If the table is empty, MAX() returns a row with a null value instead of throwing an exception
            return (maxTimestamp != null) ? maxTimestamp : new Date(0);
        } catch (NoResultException e) {
            return new Date(0);
        }
    }
}
