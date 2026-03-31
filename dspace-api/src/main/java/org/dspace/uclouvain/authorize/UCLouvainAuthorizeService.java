/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authorize;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

public interface UCLouvainAuthorizeService {
    public boolean authorizeActionBoolean(Context context, DSpaceObject dso, int action, EPerson e);
}
