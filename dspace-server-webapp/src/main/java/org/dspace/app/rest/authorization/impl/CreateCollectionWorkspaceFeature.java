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
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AuthorizationFeatureDocumentation(
    name = CreateCollectionWorkspaceFeature.NAME,
    description = "It can be used to verify if a user can create a new workspace into a specific collection."
)
public class CreateCollectionWorkspaceFeature implements AuthorizationFeature {

    public static final String NAME = "canCreateCollectionWorkspace";

    @Autowired
    private Utils utils;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException, SearchServiceException {
        if (object instanceof CollectionRest) {
            try {
                Collection collection = (Collection)utils.getDSpaceAPIObjectFromRest(context, object);
                return AuthorizeUtil.canCreateNewWorkspaceInCollection(context, collection);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { CollectionRest.CATEGORY + "." + CollectionRest.NAME };
    }
}
