package org.dspace.uclouvain.core.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/** 
 * Set of util methods for an `Item` object.
 *
 * @Author: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
*/
public class ItemUtils {

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private XmlWorkflowItemService xmlWorkflowItemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private CollectionRoleService collectionRoleService;

    /** 
    * This method is used to extract the files's bit stream from an item.
    * 
    * @param DSpaceItem: The item to extract files from.
    * @return The list of bit streams for the given item.
    */
    public static List<Bitstream> extractItemFiles(Item DSpaceItem) {
        // Configuration which gives the bundles names to use. 
        List<String> acceptedBundles = Arrays.asList(
            DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("uclouvain.resource_policy.accepted_bundles")
        );
        List<Bitstream> bitstreams = new ArrayList<Bitstream>();
        for (Bundle bundle: DSpaceItem.getBundles()) {
            if (acceptedBundles.contains(bundle.getName())) {
                bitstreams.addAll(bundle.getBitstreams());
            }
        }
        return bitstreams;
    }

    /**
     * Returns the list of all valid manager for a given item.
     * @param context: The current DSpace context.
     * @param item: The item to get the managers from.
     * @return The list of all valid managers for the given item.
     * @throws SQLException
     */
    public List<EPerson> getManagersOfItem(Context context, Item item) throws SQLException {
        List<EPerson> managers = new ArrayList<EPerson>();
        Collection collection = item.getOwningCollection();
        
        if (collection == null) {
            // Check if the item is in the workflow, if it is we need to use the XmlWorkflowItem to retrieve the owning collection.
            XmlWorkflowItem xmlWorkflowItem = this.xmlWorkflowItemService.findByItem(context, item);
            if (xmlWorkflowItem != null) {
                collection = xmlWorkflowItem.getCollection();
            }
        }

        // Retrieve all the roles created for the item's collection.
        for (CollectionRole role : this.collectionRoleService.findByCollection(context, collection)) {
            if (role.getRoleId().equals(CollectionRoleService.LEGACY_WORKFLOW_STEP1_NAME)) {
                for (Group group: role.getGroup().getMemberGroups()){
                    managers.addAll(group.getMembers());
                }
            }
        }
        return managers;
    }

    /** 
    * This method allows to get the root item of a bitstream.
    * 
    * @param Context: The current Dspace context.
    * @param Bitstream: The bitstream to get the item from.
    * @return The item that contains the given bitstream or null if none.
    * @throws SQLException
    */
    public Item getItemFromBitstream(Context context, Bitstream bitstream) throws SQLException {
        DSpaceObject dso = this.bitstreamService.getParentObject(context, bitstream);
        if (dso instanceof Item) {
            return (Item) dso;
        }
        return null;
    }

    /**
     * Checks if an item is in workflow validation.
     * @param context The current DSpace context.
     * @param item The item to check.
     * @return True if the item is in workflow false otherwise.
     * @throws SQLException
     * 
     */
    public boolean isWorkflow(Context context, Item item) throws SQLException {
        return this.xmlWorkflowItemService.findByItem(context, item) != null;
    }

    /**
     * Checks if the item is in the workspace.
     * @param context The current DSpace context.
     * @param item The item to check.
     * @return True if the item is in workspace false otherwise.
     * @throws SQLException
     */
    public boolean isWorkspace(Context context, Item item) throws SQLException {
        return this.workspaceItemService.findByItem(context, item) != null;
    }
}
