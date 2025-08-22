/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester.actions.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.profileIngester.actions.ProfileAction;

public abstract class ProfileActionFactory {
    /**
     * Get a specific action class for a given event action.
     * @param action The event action to get a class for.
     * @return The class for the corresponding event action.
     */
    public abstract ProfileAction getProfileActionClass(String action);
    public static ProfileActionFactory getInstance() {
        return DSpaceServicesFactory
                .getInstance()
                .getServiceManager()
                .getServiceByName("profileActionFactory", ProfileActionFactory.class);
    }
}
