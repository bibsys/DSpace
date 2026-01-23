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
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.GroupService;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Feature to check if a user can export a bibliography related to a specific person profile.
 */
@Component
@AuthorizationFeatureDocumentation(
        name = CanExportPersonBibliography.NAME,
        description = "It can be used to verify if a user can export a bibliography about a person"
)
public class CanExportPersonBibliography implements AuthorizationFeature {

    private final static Logger logger = LogManager.getLogger(CanExportPersonBibliography.class);
    public static final String NAME = "canExportPersonBibliography";
    private static final String CONFIG_KEY = "uclouvain.export-person-bibliography.allowed-groups";

    @Autowired
    private Utils utils;
    @Autowired
    private ItemService itemService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private ConfigurationService configService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        try {
            DSpaceObject dsObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);
            Item item = itemService.find(context, dsObject.getID());
            if (item == null) {
                return false;
            }
            return isAllowedToExportBibliography(context, item);
        } catch (Exception e) {
            logger.warn("Could not check about '%s'".formatted(CanExportPersonBibliography.NAME), e);
            return false;
        }
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }

    /**
     * Determine if the current logged user can export a bibliography
     * @param context The Dspace application context
     * @param item the research profile item related to the request (button appears on ResearchProfile detail page)
     * @return true if the current logged user can export, false otherwise
     * @throws SQLException for any database exception
     * @throws AuthorizeException for any authorization access exception
     */
    private boolean isAllowedToExportBibliography(Context context, Item item) throws SQLException, AuthorizeException {
        // Who can export a person bibliography :
        //    * Any administrators
        //    * Users belonging to allowed groups
        //    * The "owner" the researcher profile
        return authorizeService.isAdmin(context, item)
            || isMemberOfAllowedGroup(context)
            || isOwnerOfProfile(context, item);
    }

    private boolean isMemberOfAllowedGroup(Context context) {
        return Arrays
            .stream(configService.getArrayProperty(CONFIG_KEY))
            .anyMatch(groupName -> {
                try {
                    return groupService.isMember(context, groupName);
                } catch (SQLException e) {
                    return false;
                }
            });
    }

    private boolean isOwnerOfProfile(Context context, Item item) throws SQLException, AuthorizeException {
        EPerson user = context.getCurrentUser();
        if (user == null) {
            return false;
        }
        ResearcherProfile profile = researcherProfileService.findById(context, user.getID());
        return profile != null && Objects.equals(profile.getItem().getID(), item.getID());
    }
}
