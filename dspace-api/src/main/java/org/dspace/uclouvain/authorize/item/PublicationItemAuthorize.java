/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authorize.item;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.core.utils.AuthorizationUtils;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.dspace.uclouvain.services.PublicationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UCLouvain's authorize check for item objects.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class PublicationItemAuthorize {

    @Autowired
    private PublicationService publicationService;

    public boolean authorizeActionBoolean(Context context, Item item, int action, EPerson user) {
        // To manage an item, user has to be either submitter, author or manager.
        switch (action) {
            case Constants.ADD:
            case Constants.READ:
            case Constants.WRITE:
            case Constants.DELETE:
            case Constants.REMOVE:
                return user != null && isAuthorized(context, item, user);
            default:
                return false;
        }
    }

    /**
     * Authorize an action on an item: to allow a modification user should be:
     *   - the submitter
     *   - member of 'Manager' group
     *   - author of the publication.
     *
     * @param context The current DSpace application context.
     * @param item The item to check authorization of.
     * @param user The user that wants to perform an action.
     * @return True if the user is authorized, false otherwise.
     */
    private boolean isAuthorized(Context context, Item item, EPerson user) {
        if (ItemUtils.isSubmitter(context, user, item) || AuthorizationUtils.isManager(context, user)) {
            return true;
        }
        try {
            return publicationService.isAuthorOfPublication(context, item);
        } catch (SQLException | AuthorizeException e) {
            return false;
        }
    }
}
