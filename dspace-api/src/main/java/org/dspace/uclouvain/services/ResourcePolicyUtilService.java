/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.constants.AccessConditions;
import org.dspace.uclouvain.core.model.ResourcePolicyRestModel;
import org.dspace.uclouvain.core.model.ResourcePolicyRestResponse;
import org.dspace.uclouvain.core.utils.ItemUtils;

/**
 * Service to handle different operations related to resource policies.
 */
public class ResourcePolicyUtilService {
    /**
     * 'typeList' controls the handled access types for a resource policy.
     * The order matters and sets the 'weight' of each access type.
     */
    private List<String> typeList;


    /**
     * Retrieve a 'ResourcePolicyRestResponse' object from a list of resource policies.
     * This object contains the list of resource policies converted to a list of 'ResourcePolicyRestModel'
     * and a 'masterPolicy'.
     */
    public ResourcePolicyRestResponse getRestResponse(List<ResourcePolicy> rp) {
        ResourcePolicyRestResponse rpRestResponse = new ResourcePolicyRestResponse(rp, ResourcePolicy.TYPE_CUSTOM);
        rpRestResponse.masterPolicy = this.getMasterPolicy(rpRestResponse);
        return rpRestResponse;
    };

    /**
     * Finds out the main policy among the list.
     * The logic to give the primary policy is influenced by the 'typeList'.
     *
     * @param rpRestResponse An object containing the list of all the resource policies about a bitstream.
     * @return The more restrictive `ResourcePolicyRestModel` (aka master policy)
     */
    private ResourcePolicyRestModel getMasterPolicy(ResourcePolicyRestResponse rpRestResponse) {
        Date currentDate = new Date();
        return rpRestResponse.restPolicies
            .stream()
            // Keep only active policies depending on policy dates
            .filter(policy -> {
                if (policy.name.equalsIgnoreCase("embargo") && policy.startDate != null) {
                    return currentDate.getTime() <= policy.startDate.getTime();
                }
                if (policy.name.equalsIgnoreCase("lease") && policy.endDate != null) {
                    return currentDate.getTime() <= policy.endDate.getTime();
                }
                return true;
            })
            // Sort policies based on ``this.typeList`` weight
            .sorted((policyA, policyB) -> {
                int policyAWeight = this.typeList.indexOf(policyA.name);
                int policyBWeight = this.typeList.indexOf(policyB.name);
                return (policyBWeight == policyAWeight) ? 0 : policyBWeight - policyAWeight;
            })
            .findFirst().orElse(null);
    }

    /**
     * Extract a list of access types from the bitstreams of an item.
     *
     * @param ctx The current DSpace context.
     * @param item The item to extract bitstream from.
     * @return A list of access types for the given item.
     */
    public List<String> extractItemAccessTypes(Context ctx, Item item) {
        List<ResourcePolicy> allItemResourcePolicies = new ArrayList<>();
        for (Bitstream bs: ItemUtils.extractItemFiles(item)) {
            allItemResourcePolicies.addAll(bs.getResourcePolicies());
        }
        // Return only the "rpname" of the resource policies which have the type "custom" and are in the list of
        // controlled access types.
        // RP with "custom" type are policies that are assigned by the user in the file form.
        return allItemResourcePolicies
                .stream()
                .filter(rp -> rp.getRpType().equals(ResourcePolicy.TYPE_CUSTOM) && typeList.contains(rp.getRpName()))
                .map(ResourcePolicy::getRpName)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve the global access type for a given list of access types.
     *
     * @param accessTypes The list of all the different access types.
     * @return The processed global access type.
     */
    public String getGlobalAccessType(List<String> accessTypes) {
        if (accessTypes.isEmpty()) {
            return null;
        }
        // filter to keep only distinct values
        accessTypes = accessTypes.stream().distinct().collect(Collectors.toList());
        // * If the list contains only 1 value, return this value as global access type.
        // * If the list contains only `embargo` and `OpenAccess`, return OA because when the embargo should be over,
        //   then all bitstream will be OA.
        // * In case multiple access conditions are detected, return `mixed`.
        if (accessTypes.size() == 1) {
            return accessTypes.get(0);
        } else if (accessTypes.size() == 2 &&
                   accessTypes.contains(AccessConditions.EMBARGO) &&
                   accessTypes.contains(AccessConditions.OPEN_ACCESS)) {
            return AccessConditions.OPEN_ACCESS;
        } else {
            return AccessConditions.MIXED;
        }
    }

    // Getters && Setters
    public List<String> getTypeList() {
        return this.typeList;
    }
    public void setTypeList(List<String> typeList) {
        this.typeList = typeList;
    }
}
