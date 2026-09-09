/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authorize.bundle;

import java.sql.SQLException;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BundleService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bundle custom authorization override.
 * The rights a user has on an item apply to its bundles.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class BundleAuthorize {

    @Autowired
    private BundleService bundleService;
    @Autowired
    private AuthorizeService authorizeService;

    public boolean authorizeActionBoolean(Context context, Bundle bundle, int action, EPerson user) {
        switch (action) {
            case Constants.ADD:
            case Constants.READ:
            case Constants.WRITE:
            case Constants.DELETE:
            case Constants.REMOVE:
                return user != null && isAuthorized(context, bundle, action, user);
            default:
                return false;
        }
    }

    /**
     * Authorize an action on a bundle based on the permission the user has on the item.
     * 
     * @param context The current DSpace application context.
     * @param bundle The bundle to check authorization of.
     * @param action The action to check.
     * @param user The user that wants to perform an action.
     * @return True if the user is authorized, false otherwise.
     */
    private boolean isAuthorized(Context context, Bundle bundle, int action, EPerson user) {
        try {
            DSpaceObject parent = bundleService.getParentObject(context, bundle);
            if (!(parent instanceof Item item)) {
                return false;
            }
            return authorizeService.authorizeActionBoolean(context, user, item, action, false);
        } catch (SQLException e) {
            return false;
        }
    }
}
