/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Group security component to check for group membership.
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@Component(value = "groupSecurity")
public class GroupSecurityEvaluator {

    @Autowired
    private ConfigurationService configService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private HttpServletRequest request;
    @Autowired
    private AuthorizeService authorizeService;

    /**
     * Check whether the current DSpace user is a member of at least one of the given groups.
     * An admin user is considered a member of any group.
     * Intended for use in Spring Security SpEL expressions.
     *
     * @param groupNames one or more group names
     * @return true if the current user belongs to any of the groups, false otherwise or in case of error
     */
    public boolean isMemberOf(String... groupNames) {
        Context context = ContextUtil.obtainContext(request);
        if (context == null || context.getCurrentUser() == null) {
            return false;
        }
        try {
            if (authorizeService.isAdmin(context)) {
                return true;
            }
            List<Group> userGroups = groupService.allMemberGroups(context, context.getCurrentUser());
            Set<String> groupNameSet = Set.of(groupNames);
            return userGroups.stream().anyMatch(g -> groupNameSet.contains(g.getName()));
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Check whether the current DSpace user could be considered as a "Manager"
     * @return true is the current logged user is a manager, false otherwise
     */
    public boolean isManager() {
        String[] managerGroups = configService.getArrayProperty("uclouvain.feature.roles.manager", new String[] {});
        return isMemberOf(managerGroups);
    }
}
