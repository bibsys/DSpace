/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authorize.eperson;

import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.core.utils.AuthorizationUtils;

/**
 * UCLouvain's authorize check for EPerson objects.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class EPersonAuthorize {

    public boolean authorizeActionBoolean(Context context, EPerson targetUser, int action, EPerson user) {
        switch (action) {
            case Constants.READ:
                // Only allow managers and delegators to READ EPerson. (typically for submitter context)
                return user != null &&
                    (AuthorizationUtils.isManager(context, user) || AuthorizationUtils.isDelegator(context, user));
            default:
                return false;
        }
    }
}
