/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.model.WorkflowItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.core.utils.AuthorizationUtils;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Feature to check if a user can trigger the sending of attestation emails.
 * User can trigger if he is an admin or a manager for the item.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@Component
@AuthorizationFeatureDocumentation(name = CanSendAttestationEmailsFeature.NAME,
        description = "Used to verify if the given user can subscribe to a DSpace object")
public class CanSendAttestationEmailsFeature implements AuthorizationFeature {
    public static final String NAME = "canSendAttestationEmails";

    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private AuthorizationUtils authUtils;
    @Autowired
    private Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException, SearchServiceException {
        EPerson currentUser = context.getCurrentUser();
        DSpaceObject dSpaceObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);

        if (!(dSpaceObject instanceof Item)) {
            return false;
        }
        Item item = (Item) dSpaceObject;

        // Check that the item is in workflow state.
        if (!ItemUtils.isWorkflow(context, item)) {
            return false;
        }
        // Check that the user is an admin or a valid manager for the item.
        return authorizeService.isAdmin(context, currentUser)
            || authUtils.isManagerOfItem(context, item, currentUser)
            || authUtils.isLibrarian(context, currentUser);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            ItemRest.CATEGORY + "." + ItemRest.NAME,
            WorkflowItemRest.CATEGORY + "." + WorkflowItemRest.NAME
        };
    }
}
