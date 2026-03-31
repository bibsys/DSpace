/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.Arrays;

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
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.uclouvain.core.utils.AuthorizationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Feature to check if a user can see comments related to an item.
 * Typically only admins and managers of the item can see item comments.
 */
@Component
@AuthorizationFeatureDocumentation(
        name = CanSeeCommentFeature.NAME,
        description = "It can be used to verify if a user can see comments related to an item."
)
public class CanSeeCommentFeature implements AuthorizationFeature {

    private final static Logger logger = LogManager.getLogger(CanSeeCommentFeature.class);
    public static final String NAME = "canSeeComment";

    @Autowired
    private Utils utils;
    @Autowired
    private ItemService itemService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ConfigurationService configService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException, SearchServiceException {
        try {
            DSpaceObject dsObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);
            Item item = itemService.find(context, dsObject.getID());
            if (item == null) {
                return false;
            }
            EPerson currentUser = context.getCurrentUser();
            String[] authorizedGroups = configService
                    .getArrayProperty("comments.feature.authorized-group", new String[] {});
            return authorizeService.isAdmin(context)
                || AuthorizationUtils.isManagerOfItem(context, item, currentUser)
                || Arrays.stream(authorizedGroups).anyMatch(g -> safeIsMember(context, currentUser, g));
        } catch (Exception e) {
            logger.warn("Could not check for comment visibility", e);
            return false;
        }
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }

    private boolean safeIsMember(Context context, EPerson user, String groupName) {
        try {
            return groupService.isMember(context, user, groupName);
        } catch (SQLException ignored) {
            return false;
        }
    }
}
