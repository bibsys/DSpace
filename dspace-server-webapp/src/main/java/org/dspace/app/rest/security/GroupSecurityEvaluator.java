/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.utils.AuthorizationUtils;
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
    private HttpServletRequest request;
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
        return AuthorizationUtils.isMemberOf(context, context.getCurrentUser(), groupNames);
    }

    /**
     * Check whether the current DSpace user could be considered as a "Manager"
     * @return true is the current logged user is a manager, false otherwise
     */
    public boolean isManager() {
        Context context = ContextUtil.obtainContext(request);
        return AuthorizationUtils.isManager(context, context.getCurrentUser());
    }

    /**
     * Check whether the current DSpace user could be considered as a "Manager"
     * @return true is the current logged user is a manager, false otherwise
     */
    public boolean isDelegator() {
        Context context = ContextUtil.obtainContext(request);
        return AuthorizationUtils.isDelegator(context, context.getCurrentUser());
    }
}
