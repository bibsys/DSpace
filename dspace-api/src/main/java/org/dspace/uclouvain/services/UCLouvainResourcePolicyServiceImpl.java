/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import static org.dspace.authorize.ResourcePolicy.TYPE_CUSTOM;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.ResourcePolicyPriority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link UCLouvainResourcePolicyService}.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */

@Service
public class UCLouvainResourcePolicyServiceImpl implements UCLouvainResourcePolicyService {

    @Autowired
    private ResourcePolicyService resourcePolicyService;
    @Autowired
    private ResourcePolicyPriority defaultResourcePolicyPriority;

    private List<ResourcePolicyPriority> resourcePolicyPriorities;


    /** Find all valid resource policies specifically created on a DSpace object.
     *
     * @param context The application context
     * @param dso     The DspaceObject to analyze
     * @return corresponding resource policies.
     * @throws SQLException if database error
     */
    public List<ResourcePolicy> find(Context context, DSpaceObject dso) throws SQLException {
        return resourcePolicyService.find(context, dso)
                .stream()
                .filter(p -> p.getAction() == Constants.READ)
                .filter(p -> p.getRpType().equals(TYPE_CUSTOM))
                .filter(this::isValidDateInterval)
                .collect(Collectors.toList());
    }

    /** Select the master resource policy into a list of resource policies.
     *  The master policy is defined depending on `rpPriorities` attributes and based on policy name.
     *
     * @param policies The list of resource policies to analyze.
     * @return the master resource policy into this list. Could be `null` if `policies` argument is an empty list.
     */
    public ResourcePolicy getMasterPolicy(List<ResourcePolicy> policies) {
        ResourcePolicy masterPolicy = null;
        int currentMaxWeight = Integer.MIN_VALUE;
        for (ResourcePolicy policy : policies) {
            int policyWeight = getPolicyWeight(policy.getRpName());
            if (policyWeight > currentMaxWeight) {
                currentMaxWeight = policyWeight;
                masterPolicy = policy;
            }
        }
        return masterPolicy;
    }

    private boolean isValidDateInterval(ResourcePolicy policy) {
        // In a resourcePolicy, the `startDate` field is used to expose "start date where access is granted";
        // opposite, the `endDate` field is used to expose "date where access is revoked". So to test if the
        // policy is valid, we need to check endDate <= currentDate <= startDate
        long currentTime = new Date().getTime();
        long startDate = ((policy.getStartDate() != null) ? policy.getStartDate() : new Date(Long.MAX_VALUE)).getTime();
        long endDate = ((policy.getEndDate() != null) ? policy.getEndDate() : new Date(0)).getTime();
        return endDate <= currentTime && currentTime <= startDate;
    }

    /** Get the policy weight based on a policy name
     *
     * @param rpName the resource policy name
     * @return the priority weight
     */
    public int getPolicyWeight(String rpName) {
        return this.resourcePolicyPriorities
                .stream()
                .filter(p -> p.getRpName().equalsIgnoreCase(rpName))
                .findFirst()
                .orElse(defaultResourcePolicyPriority)
                .getWeight();
    }

    // GETTER & SETTER =================================================================================================
    public void setResourcePolicyPriorities(List<ResourcePolicyPriority> resourcePolicyPriorities) {
        this.resourcePolicyPriorities = resourcePolicyPriorities;
    }
}
