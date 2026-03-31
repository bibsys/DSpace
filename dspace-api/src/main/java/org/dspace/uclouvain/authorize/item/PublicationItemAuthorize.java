/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authorize.item;

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
 */
public class PublicationItemAuthorize {

    @Autowired
    private PublicationService publicationService;

    public boolean authorizeActionBoolean(Context context, Item item, int action, EPerson user) {
        switch (action) {
            case Constants.WRITE, Constants.READ:
                // For READ and WRITE, user has to be either submitter, author or manager.
                return user != null && (ItemUtils.isSubmitter(context, user, item) ||
                    isAuthor(context, item) ||
                    AuthorizationUtils.isManager(context, user));
            default:
                return false;
        }
    }

    private boolean isAuthor(Context context, Item item) {
        try {
            return publicationService.isAuthorOfPublication(context, item);
        } catch (Exception e) {
            return false;
        }
    }
}
