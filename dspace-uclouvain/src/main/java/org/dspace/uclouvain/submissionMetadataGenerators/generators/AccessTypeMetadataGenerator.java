package org.dspace.uclouvain.submissionMetadataGenerators.generators;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.constants.AccessConditions;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.submissionMetadataGenerators.exceptions.GeneratorProcessException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccessTypeMetadataGenerator implements MetadataGenerator {
    
    private String NAME = "accessTypeMetadataGenerator";
    private String MAIN_BUNDLE_NAME = "ORIGINAL";
    private List<String> accessTypes = new ArrayList<String>();
    private MetadataField accessTypeField;

    @Override
    public String getGeneratorName() {
        return this.NAME;
    }

    @Override
    public void process(Context ctx, Item item) throws GeneratorProcessException {
        try {
            ItemService currentItemService = item.getItemService();
            List<String> accessTypesRP = this.extractFileAccessTypeList(ctx, item);
            String globalAccessType = this.getGlobalAccessType(accessTypesRP);
            if (globalAccessType != null) {
                currentItemService.setMetadataSingleValue(ctx, item,
                        this.accessTypeField.getSchema(),
                        this.accessTypeField.getElement(),
                        this.accessTypeField.getQualifier(),
                        null,
                        globalAccessType
                );
            }
        } catch (SQLException e) {
            throw new GeneratorProcessException("An error occurred while updating access type metadata", e);
        } catch (Exception e) {
            throw new GeneratorProcessException("An unhandled exception occurred", e);
        }
    }

    /**
     * Extract the list of access types from the item's bitstreams
     * @param ctx: The DSpace context
     * @param item: The item to process
     * @return A list of access types
     */
    private List<String> extractFileAccessTypeList(Context ctx, Item item) {
        List<Bundle> bundles = item.getBundles();
        List<ResourcePolicy> allItemResourcePolicies = new ArrayList<ResourcePolicy>();
        for (Bundle bundle: bundles) {
            // Retrieve bitstreams from the main file bundle
            if (bundle.getName().equals(MAIN_BUNDLE_NAME)) {
                // For each bitstream, add resource policies to the list
                for (Bitstream bs: bundle.getBitstreams()) {
                    allItemResourcePolicies.addAll(bs.getResourcePolicies());
                }
            }
        }
        // Return only the "rpname" of the resource policies which have the type "custom" and are in the list of controlled access types 
        return allItemResourcePolicies.stream().filter(
            (rp) -> {
                return rp.getRpType().equals(ResourcePolicy.TYPE_CUSTOM) && this.accessTypes.contains(rp.getRpName());
            }
        ).map(
            rp -> rp.getRpName()
        ).collect(Collectors.toList());
    }

    /**
     * Retrieve the global access type for a given list of access types
     * @param accessTypes: The list of all the different access types
     * @return The processed global access type
     */
    private String getGlobalAccessType(List<String> accessTypes) {
        if (accessTypes.isEmpty()){
            return null;
        }
        // filter to keep only distinct values
        accessTypes = accessTypes.stream().distinct().collect(Collectors.toList());
        // * If list contains only 1 value, just return this value as global access type
        // * If list contains only `embargo` and `openaccess`, just return OA because when embargo will be over, then all
        //   bitstream will be OA.
        // * Otherwise, multiple access conditions are detected, just return `mixed`
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
    public List<String> getAccessTypes() {
        return this.accessTypes;
    }

    public void setAccessTypes(List<String> accessTypes) {
        this.accessTypes = accessTypes;
    }

    public MetadataField getAccessTypeField() {
        return this.accessTypeField;
    }

    public void setAccessTypeField(MetadataField accessTypeField) {
        this.accessTypeField = accessTypeField;
    }
}
