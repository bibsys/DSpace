/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.dao.impl;

import java.sql.SQLException;

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
}
