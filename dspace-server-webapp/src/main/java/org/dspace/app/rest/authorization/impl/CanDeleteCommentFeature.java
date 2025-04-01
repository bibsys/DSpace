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
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Feature to check if a user can delete comments related to an item.
 * Typically only admins can delete item comments.
 */
@Component
@AuthorizationFeatureDocumentation(
        name = CanDeleteCommentFeature.NAME,
        description = "It can be used to verify if a user can delete comments related to an item."
)
public class CanDeleteCommentFeature implements AuthorizationFeature {

    private final static Logger logger = LogManager.getLogger(CanDeleteCommentFeature.class);
    public static final String NAME = "canDeleteComment";

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        try {
            return authorizeService.isAdmin(context);
        } catch (Exception e) {
            logger.warn("Could not check for comment deletion", e);
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
