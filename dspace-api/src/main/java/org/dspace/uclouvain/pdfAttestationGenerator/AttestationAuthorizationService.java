package org.dspace.uclouvain.pdfAttestationGenerator;

import java.sql.SQLException;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.uclouvain.pdfAttestationGenerator.configuration.PDFAttestationGeneratorConfiguration;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

// Service to check permission for attestation generation and download.
public class AttestationAuthorizationService {

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private XmlWorkflowItemService xmlWorkflowItemService;

    @Autowired
    private CollectionRoleService collectionRoleService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private PDFAttestationGeneratorConfiguration pdfAttestationGeneratorConfiguration;

    /**
     * Check if the item is valid for the current attestation configuration.
     * @param item: The item to check.
     * @param context: The current DSpace context.
     * @return True if the item is valid for at least one of the configured attestation.
     * @throws SQLException
     */
    public Boolean isItemValidForAttestation(Item item, Context context) throws SQLException {
        // Check that the item is not in the workspace
        return workspaceItemService.findByItem(context, item) == null 
            && this.pdfAttestationGeneratorConfiguration
                .getAllHandledTypes()
                .contains(
                    itemService.getMetadataFirstValue(
                        item, 
                        "dspace", 
                        "entity", 
                        "type", 
                        Item.ANY
                    )
                );
    }

    /**
     * Check if the user is authorized to download the attestation.
     * Authorized if:
     * - the item can be handled by one of the available handler AND (
     *     - the user is an admin OR
     *     - the user is the submitter of the item OR
     *     - the user is a manager
     * )
     * @param dsItem: The item to check permission for.
     * @param ctx: The current DSpace context.
     * @return True if the user is validates one of the above conditions.
     * @throws SQLException
     */
    public Boolean isUserAuthorized(Item dsItem, Context ctx) throws SQLException {
        EPerson currentUser = ctx.getCurrentUser();
        if (currentUser == null) return false; 
        return (authorizeService.isAdmin(ctx, dsItem))
            || (dsItem.getSubmitter() == currentUser)
            || (this.isManagerOfItem(dsItem, currentUser, ctx));
    }

    /**
     * Checks if the current user is a manager of the item's collection.
     * Retrieve the group for the collection that corresponds to the reviewer role and check if the current user is a member.
     * @param item: The item to check permission for.
     * @param currentUser: The current logged user.
     * @param ctx: The current DSpace context.
     * @return True if the user is a manager of the item's collection else false.
     * @throws SQLException
     */
    public Boolean isManagerOfItem(Item item, EPerson currentUser, Context ctx) throws SQLException {
        Collection collection = item.getOwningCollection();
        if (collection == null) {
            // Check if the item is in the workflow, if it is we need to use the XmlWorkflowItem to retrieve the owning collection.
            XmlWorkflowItem xmlWorkflowItem = xmlWorkflowItemService.findByItem(ctx, item);
            if (xmlWorkflowItem != null) {
                collection = xmlWorkflowItem.getCollection();
            };
        }

        // Retrieve all the roles created for the item's collection.
        for (CollectionRole role : collectionRoleService.findByCollection(ctx, collection)) {
            if (role.getRoleId().equals(CollectionRoleService.LEGACY_WORKFLOW_STEP1_NAME)) {
                for (Group group: role.getGroup().getMemberGroups()){
                    if (group.getMembers().contains(currentUser)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
