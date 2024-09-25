package org.dspace.app.rest.authorization.impl;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.EPersonRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
@AuthorizationFeatureDocumentation(
    name = HasRoleManagerFeature.NAME,
    description = "Allow to determine if the current logged used has the `manager` role"
)
public class HasRoleManagerFeature implements AuthorizationFeature {

    public static final String NAME = "hasRoleManager";

    @Autowired
    private Utils utils;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ConfigurationService configurationService;

    /**
     * This method checks if a user belongs to `manager` groups
     * @param context: The current DSpace context.
     * @param object: The user to check
     * @return True if the user could be considerate as a manager
     */
    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        if (!(object instanceof EPersonRest)) {
            return false;
        }
        EPerson ePerson = (EPerson) utils.getDSpaceAPIObjectFromRest(context, object);
        String[] managerRoles = this.configurationService.getArrayProperty("uclouvain.feature.roles.manager", new String[0]);
        for(String role: managerRoles) {
            if (groupService.isMember(context, ePerson, role)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method lists the supported types for this feature.
     * In our case, it is only the ePerson type.
     */
    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            EPersonRest.CATEGORY + "." + EPersonRest.NAME
        };
    }

}
