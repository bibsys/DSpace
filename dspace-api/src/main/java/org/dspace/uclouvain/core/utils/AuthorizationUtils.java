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
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Utility class for authorization checks.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class AuthorizationUtils {

    @Autowired
    private ItemUtils itemUtils;

    @Autowired
    private ConfigurationService configService;

    @Autowired
    private GroupService groupService;

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

    /**
     * Check if a user is a librarian by checking his group membership.
     * @param context The current DSpace context.
     * @param currentUser The user to evaluate.
     * @return True if the user is a librarian, false otherwise.
     */
    public boolean isLibrarian(Context context, EPerson user) {
        String[] librarianGroups = this.configService
            .getArrayProperty("uclouvain.feature.roles.librarian", new String[0]);
        return isMemberOfGroup(context, user, librarianGroups);
    }

    /**
     * Check if a user is a manager by checking his group membership.
     * @param context The current DSpace context.
     * @param currentUser The user to evaluate.
     * @return True if the user is a manager, false otherwise.
     */
    public boolean isManager(Context context, EPerson user) {
        String[] managerGroups = this.configService
            .getArrayProperty("uclouvain.feature.roles.manager", new String[0]);
        return isMemberOfGroup(context, user, managerGroups);
    }

    /**
     * Returns whether a given user is a member of at least one of the given groups.
     * @param context The current DSpace context.
     * @param person The user to evaluate.
     * @param groupNames The names of the groups to check for membership.
     * @return True if the user is a member of at least one of the given groups. False otherwise.
     */
    private boolean isMemberOfGroup(Context context, EPerson person, String[] groupNames) {
        for (String role: groupNames) {
            try {
                if (groupService.isMember(context, person, role)) {
                    return true;
                }
            } catch (SQLException e) {
                continue;
            }
        }
        return false;
    }
}
