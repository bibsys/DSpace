package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.dspace.uclouvain.core.utils.AuthorizationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Feature to check if a user can see the full detailed version of an item.
 * Typically only admins and managers of the item can see the full item.
 */
@Component
@AuthorizationFeatureDocumentation(
    name = CanSeeFullItemFeature.NAME,
    description = "It can be used to verify if a user can see the full detailed version of an item."
)
public class CanSeeFullItemFeature implements AuthorizationFeature {
    public static final String NAME = "canSeeFullItem";

    @Autowired
    private Utils utils;

    @Autowired
    private ItemService itemService;

    @Autowired
    private AuthorizationUtils authUtils;

    @Autowired
    private AuthorizeService authorizeService;

    private static Logger logger = LogManager.getLogger(CanSeeFullItemFeature.class);

    /**
     * This method checks if a user can see the full detailed version of an item.
     * @param context: The current DSpace context.
     * @param object: The object to check authorization for.
     * @return True if the user is authorized to see the full detailed version of the item.
     * @throws SQLException If something goes wrong.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        try {
            DSpaceObject dsObject = (DSpaceObject) this.utils.getDSpaceAPIObjectFromRest(context, object);
            Item dsItem = this.itemService.find(context, dsObject.getID());
    
            if (dsItem == null) return false;
            
            EPerson currentUser = context.getCurrentUser();        
            if (this.authorizeService.isAdmin(context) || this.authUtils.isManagerOfItem(context, dsItem, currentUser)) {
                return true;
            };
            return false;
        } catch (Exception e) {
            logger.warn("Could not check for item visibility", e);
            return false;
        }

    }

    /**
     * This method lists the supported types for this feature.
     * In our case it is only the item type.
     */
    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }
    
}
