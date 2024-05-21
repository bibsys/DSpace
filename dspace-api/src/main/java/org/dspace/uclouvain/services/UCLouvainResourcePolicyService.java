package org.dspace.uclouvain.services;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * Extension of {@link org.dspace.authorize.service.ResourcePolicyService} with
 * custom methods for UCLouvain features.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */

public interface UCLouvainResourcePolicyService {

    /** Find all valid resource policies specifically created on a DSpace object.
     *
     * @param context The application context
     * @param dso     The DspaceObject to analyze
     * @return corresponding resource policies.
     * @throws SQLException if database error
     */
    List<ResourcePolicy> find(Context context, DSpaceObject dso) throws SQLException;

    /** Select the master resource policy into a list of resource policies.
     *  The master policy is defined depending on `rpPriorities` attributes and based on policy name.
     *
     * @param policies The list of resource policies to analyze.
     * @return the master resource policy into this list. Could be `null` if `policies` argument is an empty list.
     */
    ResourcePolicy getMasterPolicy(List<ResourcePolicy> policies);
}
