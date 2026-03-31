/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authorize.bundle;

import java.sql.SQLException;
import java.util.Optional;

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
 * This is necessary to allow READ right on withdrawn items bitstreams.
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
            case Constants.READ:
                // If user has read rights on item, authorize read on bundle.
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
        return getItem(context, bundle).map(
            item -> {
                try {
                    return authorizeService.authorizeActionBoolean(context, user, item, action, false);
                } catch (SQLException e) {
                    return false;
                }
            }
        ).orElse(false);
    }

    /**
     * Get the parent item of a bundle.
     * 
     * @param context The current DSpace application context.
     * @param bundle The bundle to get the owning item for.
     * @return A Nullable optional containing the owning Item.
     */
    private Optional<Item> getItem(Context context, Bundle bundle) {
        try {
            DSpaceObject parent = bundleService.getParentObject(context, bundle);
            return (parent instanceof Item item) ? Optional.ofNullable(item) : Optional.ofNullable(null);
        } catch (SQLException e) {
            return Optional.ofNullable(null);
        }
    }
}
