package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Rule to indicate who has the permission to download a given item's submission attestation
 */
@Component
@AuthorizationFeatureDocumentation(
    name = CanDownloadPDFAttestationFeature.NAME,
    description = "It can be used to verify if the attestation can be downloaded"
)
public class CanDownloadPDFAttestationFeature implements AuthorizationFeature {
    public final static String NAME = "canDownloadPDFAttestation";

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private Utils utils;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        DSpaceObject dsObject = (DSpaceObject)utils.getDSpaceAPIObjectFromRest(context, object);
        Item dsItem = itemService.find(context, dsObject.getID());
        return dsItem.isArchived() && this.isUserAuthorized(dsItem, context);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {ItemRest.CATEGORY + "." + ItemRest.NAME};
    }

    public Boolean isUserAuthorized(Item dsItem, Context ctx) throws SQLException {
        EPerson currentUser = ctx.getCurrentUser();
        if (currentUser == null) return false; 
        return authorizeService.isAdmin(ctx, dsItem) || (dsItem.getSubmitter() == ctx.getCurrentUser());
    }
}
