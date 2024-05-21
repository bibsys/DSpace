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
     * Finds out the main policy among a list. The logic to give the master policy is influenced by the 'typeList'.
     * @param rpRestResponse: A object containing the list of all the resource policy of a bitstream.
     * @return The more restrictive `ResourcePolicyRestModel` (aka master policy)
     */
    private ResourcePolicyRestModel getMasterPolicy(ResourcePolicyRestResponse rpRestResponse) {
        Date currentDate = new Date();
        return rpRestResponse.restPolicies
            .stream()
            // Keep only active policies depending on policy dates
            .filter(policy -> {
                Date startDate = (policy.startDate != null) ? policy.startDate : new Date(0);
                Date endDate = (policy.endDate != null) ? policy.endDate : new Date(Long.MAX_VALUE);
                return (startDate.getTime() <= currentDate.getTime() && currentDate.getTime() <= endDate.getTime());
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
     * @param ctx: The current DSpace context.
     * @param item: The item to extract bitstream from.
     * @return A list of access types for the given item.
     */
    public List<String> extractItemAccessTypes(Context ctx, Item item) {
        List<ResourcePolicy> allItemResourcePolicies = new ArrayList<ResourcePolicy>();
        for (Bitstream bs: ItemUtils.extractItemFiles(item)) {
            allItemResourcePolicies.addAll(bs.getResourcePolicies());
        }
        // Return only the "rpname" of the resource policies which have the type "custom" and are in the list of controlled access types.
        // RP that have the type "custom" are the one that are assigned by the user in the file form.
        return allItemResourcePolicies
                .stream()
                .filter(p -> p.getRpType().equals(ResourcePolicy.TYPE_CUSTOM) && this.typeList.contains(p.getRpName()))
                .map(ResourcePolicy::getRpName)
                .collect(Collectors.toList());
    }

    /**
     * Retrieve the global access type for a given list of access types.
     * @param accessTypes: The list of all the different access types.
     * @return The processed global access type.
     */
    public String getGlobalAccessType(List<String> accessTypes) {
        if (accessTypes.isEmpty()){
            return null;
        }
        // filter to keep only distinct values
        accessTypes = accessTypes.stream().distinct().collect(Collectors.toList());
        // * If list contains only 1 value, just return this value as global access type.
        // * If list contains only `embargo` and `openaccess`, just return OA because when the embargo will be over, then all
        // * bitstream will be OA.
        // * In case multiple access conditions are detected, just return `mixed`.
        if (accessTypes.size() == 1){
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
