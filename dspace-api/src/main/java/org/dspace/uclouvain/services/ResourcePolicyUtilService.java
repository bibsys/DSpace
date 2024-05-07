package org.dspace.uclouvain.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.constants.AccessConditions;
import org.dspace.uclouvain.core.model.ResourcePolicyRestModel;
import org.dspace.uclouvain.core.model.ResourcePolicyRestResponse;

/**
 * Service to handle different operations related to resource policies.
 */
public class ResourcePolicyUtilService {
    private List<String> acceptedBundles = Arrays.asList(
        DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("uclouvain.resource_policy.accepted_bundles")
    );
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
        this.setMasterPolicy(rpRestResponse);
        return rpRestResponse;
    };

    /**
     * Finds out the main policy among a list. The logic to give the master policy is influenced by the 'typeList'.
     * @param rpRestResponse: A object containing the list of all the resource policy of a bitstream.
     */
    private void setMasterPolicy(ResourcePolicyRestResponse rpRestResponse) {
        List<ResourcePolicyRestModel> activePolicies = rpRestResponse.restPolicies.stream()
            // Only keep active embargo
            .filter(
                policy -> {
                    if(policy.name.equals("embargo")){
                        long currentDate = new Date().getTime();
                        return policy.startDate != null && (currentDate < policy.startDate.getTime());
                    }
                    return true;
                }
            )
            .collect(Collectors.toList());
        List<String> policiesTypes = activePolicies.stream().map(x -> x.name).collect(Collectors.toList());

        for (String type: this.typeList) {
            if (policiesTypes.contains(type)){
                rpRestResponse.masterPolicy = this.getResourceForType(type, activePolicies);
                return;
            }
        }
    }

    private ResourcePolicyRestModel getResourceForType(String type, List<ResourcePolicyRestModel> policies){
        return policies.stream().filter(x -> x.name.equals(type)).findFirst().orElse(null);
    }

    /**
     * Extract a list of access types from the bitstreams of an item.
     * @param ctx: The current DSpace context.
     * @param item: The item to extract bitstream from.
     * @return A list of access types for the given item.
     */
    public List<String> extractItemAccessTypes(Context ctx, Item item) {
        List<ResourcePolicy> allItemResourcePolicies = new ArrayList<ResourcePolicy>();
        for (Bundle bundle: item.getBundles()) {
            // Retrieve bitstreams only from the configured bundles
            if (this.acceptedBundles.contains(bundle.getName())) {
                // For each bitstream, add resource policies to the list
                for (Bitstream bs: bundle.getBitstreams()) {
                    allItemResourcePolicies.addAll(bs.getResourcePolicies());
                }
            }
        }
        // Return only the "rpname" of the resource policies which have the type "custom" and are in the list of controlled access types.
        // RP that have the type "custom" are the one that are assigned by the user in the file form.
        return allItemResourcePolicies.stream().filter(
            (rp) -> {
                return rp.getRpType().equals(ResourcePolicy.TYPE_CUSTOM) && this.typeList.contains(rp.getRpName());
            }
        ).map(
            rp -> rp.getRpName()
        ).collect(Collectors.toList());
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
