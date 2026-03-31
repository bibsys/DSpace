/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import java.sql.SQLException;

import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Utility class for authorization checks.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class AuthorizationUtils {

    protected AuthorizationUtils () {
        throw new UnsupportedOperationException();
    }

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
    public static boolean isManagerOfItem(Context context, Item item, EPerson currentUser) throws SQLException {
        return ItemUtils.getManagersOfItem(context, item).contains(currentUser);
    }

    public static boolean isManager(Context context, EPerson user) {
        String[] managerGroups = DSpaceServicesFactory
            .getInstance()
            .getConfigurationService()
            .getArrayProperty("uclouvain.feature.roles.manager", new String[] {});
        return isMemberOf(context, user, managerGroups);
    }

    public static boolean isDelegator(Context context, EPerson user) {
        String[] delegatorGroups = DSpaceServicesFactory
            .getInstance()
            .getConfigurationService()
            .getArrayProperty("uclouvain.feature.roles.delegator", new String[] {});
        return isMemberOf(context, user, delegatorGroups);
    }

    public static boolean isMemberOf(Context context, EPerson user, String... groupNames) {
        if (context == null || user == null) {
            return false;
        }
        try {
            GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
            AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
            return authorizeService.isAdmin(context, user)
                || groupService.isMember(context, user, groupNames);
        } catch (SQLException e) {
            return false;
        }
    }
}
