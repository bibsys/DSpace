/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.dao;

import java.sql.SQLException;

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
}
