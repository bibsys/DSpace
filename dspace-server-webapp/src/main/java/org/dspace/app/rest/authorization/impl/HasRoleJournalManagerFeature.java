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
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AuthorizationFeatureDocumentation(
    name = HasRoleJournalManagerFeature.name,
    description = "Check if a user has the journal manager role by checking its groups."
)
public class HasRoleJournalManagerFeature implements AuthorizationFeature {
    public static final String name = "hasRoleJournalManager";

    @Autowired
    private Utils utils;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ConfigurationService configService;
    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest user) throws SQLException, SearchServiceException {
        if (!(user instanceof EPersonRest)) {
            return false;
        }
        EPerson ePerson = (EPerson) utils.getDSpaceAPIObjectFromRest(context, user);
        if (authorizeService.isAdmin(context)) {
            return true;
        }
        String[] journalManagerRoles =
            configService.getArrayProperty("uclouvain.feature.roles.journal-manager", new String[0]);
        for (String role: journalManagerRoles) {
            if (groupService.isMember(context, ePerson, role)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            EPersonRest.CATEGORY + "." + EPersonRest.NAME
        };
    }
}
