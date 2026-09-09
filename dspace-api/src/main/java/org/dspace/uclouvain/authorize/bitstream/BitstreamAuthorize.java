/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authorize.bitstream;

import java.sql.SQLException;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Bitstream custom authorization override.
 * The rights a user has on an item apply to its bitstreams.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class BitstreamAuthorize {

    @Autowired
    private BitstreamService bitstreamService;
    @Autowired
    private AuthorizeService authorizeService;

    public boolean authorizeActionBoolean(Context context, Bitstream bitstream, int action, EPerson user) {
        // !!! Not READ here otherwise all resourcePolicy checks should be bypassed !!!
        switch (action) {
            case Constants.ADD:
            case Constants.WRITE:
            case Constants.DELETE:
            case Constants.REMOVE:
                return user != null && isAuthorized(context, bitstream, action, user);
            default:
                return false;
        }
    }

    /**
     * Authorize an action on a bitstream based on the permission the user has on the owning item.
     *
     * @param context The current DSpace application context.
     * @param bitstream The bitstream to check authorization of.
     * @param action The action to check.
     * @param user The user that wants to perform an action.
     * @return True if the user is authorized, false otherwise.
     */
    private boolean isAuthorized(Context context, Bitstream bitstream, int action, EPerson user) {
        try {
            DSpaceObject parent = bitstreamService.getParentObject(context, bitstream);
            if (!(parent instanceof Item item)) {
                return false;
            }
            return authorizeService.authorizeActionBoolean(context, user, item, action, false);
        } catch (SQLException e) {
            return false;
        }
    }
}
