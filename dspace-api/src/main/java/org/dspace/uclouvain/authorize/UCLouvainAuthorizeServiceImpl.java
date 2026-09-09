/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authorize;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.authorize.bitstream.BitstreamAuthorize;
import org.dspace.uclouvain.authorize.bundle.BundleAuthorize;
import org.dspace.uclouvain.authorize.eperson.EPersonAuthorize;
import org.dspace.uclouvain.authorize.item.PublicationItemAuthorize;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UCLouvain authorization service to do additional checks on a given dso and eperson.
 * This is handy in some cases when DSpace checks for resource policy but we don't have any.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainAuthorizeServiceImpl implements UCLouvainAuthorizeService {

    @Autowired
    private ItemService itemService;
    @Autowired
    private PublicationItemAuthorize publicationItemAuthorize;
    @Autowired
    private EPersonAuthorize epersonAuthorize;
    @Autowired
    private BundleAuthorize bundleAuthorize;
    @Autowired
    private BitstreamAuthorize bitstreamAuthorize;

    /**
     * For a given dso, action and user, returns if the action is authorized or not.
     * 
     * @param context The current DSpace context.
     * @param dso The dso to check authorization on.
     * @param action The action to check.
     * @param user The user that wants to perform the action.
     */
    public boolean authorizeActionBoolean(Context context, DSpaceObject dso, int action, EPerson user) {
        if (dso == null) {
            return false;
        } else if (dso instanceof Item item) {
            // Only publications have custom rules; getEntityType() may be null for items without an entity type.
            return Publication.ENTITY_TYPE.equals(itemService.getEntityType(item))
                && publicationItemAuthorize.authorizeActionBoolean(context, item, action, user);
        } else if (dso instanceof EPerson eperson) {
            return epersonAuthorize.authorizeActionBoolean(context, eperson, action, user);
        } else if (dso instanceof Bundle bundle) {
            return bundleAuthorize.authorizeActionBoolean(context, bundle, action, user);
        } else if (dso instanceof Bitstream bitstream) {
            return bitstreamAuthorize.authorizeActionBoolean(context, bitstream, action, user);
        }
        return false;
    }
}
