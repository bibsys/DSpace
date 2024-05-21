/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.access.status;

import java.sql.SQLException;
import java.util.Date;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Plugin interface for the access status calculation.
 */
public interface AccessStatusHelper {
    /**
     * Calculate the access status for the item.
     *
     * @param context the DSpace context
     * @param item    the item
     * @param threshold the embargo threshold date
     * @return an access status value
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public String getAccessStatusFromItem(Context context, Item item, Date threshold)
        throws SQLException;

    /**
     * Retrieve embargo information for the item
     *
     * @param context the DSpace context
     * @param item the item to check for embargo information
     * @param threshold the embargo threshold date
     * @return an embargo date
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public String getEmbargoFromItem(Context context, Item item, Date threshold) throws SQLException;


    /**
     * Look at the DSpace object's policies to determine an access status value.
     *
     * If the object is null, returns the "metadata.only" value.
     * If any policy attached to the object is valid for the anonymous group,
     * returns the "open.access" value.
     * Otherwise, if the policy start date is before the embargo threshold date,
     * returns the "embargo" value.
     * Every other cases return the "restricted" value.
     *
     * @param context     the DSpace context
     * @param dso         the DSpace object
     * @return an access status value
     */
    public String calculateAccessStatusForDso(Context context, DSpaceObject dso) throws SQLException;
}
