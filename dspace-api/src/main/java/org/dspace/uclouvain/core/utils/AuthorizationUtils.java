/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Utility class for authorization checks.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class AuthorizationUtils {

    @Autowired
    private ItemUtils itemUtils;

    /**
     * Checks if the current user is a manager of the item's collection.
     * Retrieve the group for the collection that corresponds to the reviewer role and check if the current user is a
     * member.
     * @param item The item to check permission for.
     * @param currentUser The current logged user.
     * @param context The current DSpace context.
     * @return Returns `true` if the user is a manager of the item's collection else `false`.
     * @throws SQLException for any database exception.
     */
    public boolean isManagerOfItem(Context context, Item item, EPerson currentUser) throws SQLException {
        return this.itemUtils.getManagersOfItem(context, item).contains(currentUser);
    }
}
