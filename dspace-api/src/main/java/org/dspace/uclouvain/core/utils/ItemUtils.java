/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/** 
 * Set of util methods for an `Item` object.
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
*/
public class ItemUtils {

    @Autowired
    private BitstreamService bitstreamService;

    /** 
    * This method is used to extract all bitstreams from an item.
    * 
    * @param DSpaceItem The item to extract files from.
    * @return The list of bit streams for the given item.
    */
    public static List<Bitstream> extractItemFiles(Item DSpaceItem) {
        // Configuration which gives the bundles names to use.
        List<String> acceptedBundles = Arrays.asList(
            DSpaceServicesFactory
                    .getInstance()
                    .getConfigurationService()
                    .getArrayProperty("uclouvain.resource_policy.accepted_bundles")
        );
        List<Bitstream> bitstreams = new ArrayList<>();
        for (Bundle bundle: DSpaceItem.getBundles()) {
            if (acceptedBundles.contains(bundle.getName())) {
                bitstreams.addAll(bundle.getBitstreams());
            }
        }
        return bitstreams;
    }

    /**
     * Returns the list of all valid managers for a given item.
     * @param context The current DSpace context.
     * @param item The item to get the managers from.
     * @return The list of all valid managers for the given item.
     * @throws SQLException for any database exception
     */
    public static List<EPerson> getManagersOfItem(Context context, Item item) throws SQLException {
        Collection itemCollection = getMainCollection(context, item);
        if (itemCollection == null) {
            return Collections.emptyList();
        }

        // Use a Set to avoid duplicates if a user belongs to multiple groups
        Set<EPerson> managers = new HashSet<>();
        CollectionRoleService roleService = XmlWorkflowServiceFactory.getInstance().getCollectionRoleService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        // Retrieve all roles and filter for the specific manager role
        List<CollectionRole> roles = roleService.findByCollection(context, itemCollection);
        for (CollectionRole role : roles) {
            if (CollectionRoleService.LEGACY_WORKFLOW_STEP1_NAME.equals(role.getRoleId())) {
                Group roleGroup = role.getGroup();
                if (roleGroup != null) {
                    // allMembers handles nested groups and returns a unique list of EPeople
                    managers.addAll(groupService.allMembers(context, roleGroup));
                }
            }
        }
        return new ArrayList<>(managers);
    }

    /** 
    * This method allows getting the root item of a bitstream.
    * 
    * @param context The current Dspace context.
    * @param bitstream The bitstream to get the item from.
    * @return The item that contains the given bitstream or null if none.
    * @throws SQLException for any database exception
    */
    public Item getItemFromBitstream(Context context, Bitstream bitstream) throws SQLException {
        DSpaceObject dso = this.bitstreamService.getParentObject(context, bitstream);
        return (dso instanceof Item)
            ? (Item) dso
            : null;
    }

    /**
     * Get the main collection for a given item.
     * Depending on item life-cycle, the item's main collection should be retrieved differently.
     *
     * @param context The DSpace context.
     * @param item    The DSpace Item to analyze.
     * @return The main collection to which the item belongs. Returns null if not found.
     */
    public static Collection getMainCollection(Context context, Item item) {
        return Optional.ofNullable(item.getOwningCollection())
            .or(() -> findWorkspaceCollection(context, item))
            .or(() -> findWorkflowCollection(context, item))
            .orElse(null);
    }
    private static Optional<Collection> findWorkspaceCollection(Context context, Item item) {
        try {
            WorkspaceItem wsItem = ContentServiceFactory.getInstance()
                .getWorkspaceItemService()
                .findByItem(context, item);
            return Optional.ofNullable(wsItem).map(WorkspaceItem::getCollection);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }
    private static Optional<Collection> findWorkflowCollection(Context context, Item item) {
        try {
            XmlWorkflowItem wfItem = XmlWorkflowServiceFactory.getInstance()
                .getXmlWorkflowItemService()
                .findByItem(context, item);
            return Optional.ofNullable(wfItem).map(XmlWorkflowItem::getCollection);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    /**
     * Checks if an item is in workflow validation.
     * @param context The current DSpace context.
     * @param item The item to check.
     * @return True if the item is in workflow false otherwise.
     * @throws SQLException for any database exception
     * 
     */
    public static boolean isWorkflow(Context context, Item item) throws SQLException {
        return XmlWorkflowServiceFactory.getInstance().getWorkflowItemService().findByItem(context, item) != null;
    }

    /**
     * Checks if the item is in the workspace.
     * @param context The current DSpace context.
     * @param item The item to check.
     * @return True if the item is in workspace false otherwise.
     * @throws SQLException for any database exception
     */
    public static boolean isWorkspace(Context context, Item item) throws SQLException {
        return ContentServiceFactory.getInstance().getWorkspaceItemService().findByItem(context, item) != null;
    }
}
