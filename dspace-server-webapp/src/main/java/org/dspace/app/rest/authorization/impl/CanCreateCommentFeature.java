/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Feature to check if a user can delete comments related to an item.
 * Typically only admins can delete item comments.
 */
@Component
@AuthorizationFeatureDocumentation(
        name = CanCreateCommentFeature.NAME,
        description = "It can be used to verify if a user can create new comment related to an item."
)
public class CanCreateCommentFeature implements AuthorizationFeature {

    private final static Logger logger = LogManager.getLogger(CanCreateCommentFeature.class);
    public static final String NAME = "canCreateComment";

    @Autowired
    private Utils utils;
    @Autowired
    private ItemService itemService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private EditItemModeService modeService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        try {
            DSpaceObject dsObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);
            Item item = itemService.find(context, dsObject.getID());
            if (item == null) {
                return false;
            }
            return authorizeService.isAdmin(context) || modeService.canEdit(context, item);
        } catch (Exception e) {
            logger.warn("Could not check for comment creation", e);
            return false;
        }
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }
}
