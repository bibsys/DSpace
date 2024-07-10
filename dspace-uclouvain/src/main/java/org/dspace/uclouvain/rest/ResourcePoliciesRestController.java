package org.dspace.uclouvain.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.ResourcePolicyRestModel;
import org.dspace.uclouvain.core.model.ResourcePolicyRestResponse;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.dspace.uclouvain.services.ResourcePolicyUtilService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that can be used to retrieve resource policies information for a given dspace object.
 */
@RestController
@RequestMapping("/api/uclouvain/resourcepolicies")
public class ResourcePoliciesRestController {

    @Autowired
    private ResourcePolicyService resourcePolicyService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private ResourcePolicyUtilService resourcePolicyUtilService;

    /**
     * Endpoint to retrieve resource policies information for a given bitstream UUID.
     * @param context: The current DSpace context.
     * @param id: The UUID of the bitstream to search resource policies of.
     * @return A ResourcePolicyRestResponse object giving all the resource policies for the bitstream.
     * @throws SQLException
     * @throws IOException
     */
    @RequestMapping(value = "/bitstream/{id}", method = RequestMethod.GET)
    public ResourcePolicyRestResponse getResourcePolicies(Context context, @PathVariable UUID id, HttpServletResponse response) throws SQLException, IOException {
        Bitstream bitstream = bitstreamService.find(context, id);
        if (bitstream != null) {
            List<ResourcePolicy> rp = resourcePolicyService.find(context, bitstream);
            return this.resourcePolicyUtilService.getRestResponse(rp);
        }
        response.sendError(400, "Object not found or not a bitstream");
        return null;
    }

    /**
     * Endpoint to retrieve the main access type for a given item UUID.
     * @param context: The current DSpace context.
     * @param id: The UUID of the item to search resource policies of.
     * @return A global policy generated from all the master policies of the item's bitstreams.
     * @throws SQLException
     * @throws IOException
     */
    @RequestMapping(value = "/item/{id}", method = RequestMethod.GET)
    public HashMap<String, String> getItemGlobalPolicy(Context context, @PathVariable UUID id, HttpServletResponse response) throws SQLException, IOException {
        Item item = itemService.find(context, id);
        if (item != null) {
            List<String> rpTypes = new ArrayList<String>();
            // Retrieve all the bitstreams from the accepted bundles and get their master policy.
            for (Bitstream bs: ItemUtils.extractItemFiles(item)) {
                ResourcePolicyRestModel masterPolicy = this.resourcePolicyUtilService.getRestResponse(bs.getResourcePolicies()).masterPolicy;
                if (masterPolicy != null) {
                    rpTypes.add(masterPolicy.name);
                }
            }
    
            // Use hashmap object to return the global access type of the item in a JSON format.
            HashMap<String, String> responseMap = new HashMap<String, String>();
            // From all the master policies, generate the global access type of the item.
            responseMap.put("globalAccessType", this.resourcePolicyUtilService.getGlobalAccessType(rpTypes));
            return responseMap;
        }
        response.sendError(400, "Object not found or not an item");
        return null;
    }
}
