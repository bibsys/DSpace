/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.actions;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.plugins.UCLouvainAccessStatusHelper;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for UCLouvain thesis actions.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public abstract class UCLouvainThesisAction extends ReviewAction {
    protected final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private Logger logger = LogManager.getLogger(UCLouvainThesisAction.class);

    // Active Request Field
    protected final MetadataField activeRF = new MetadataField(configService.getProperty(
        "uclouvain.global.metadata.activerequestchange.field"));


    @Autowired
    protected AuthorizeService authorizeService;
    @Autowired
    protected WorkflowItemRoleService workflowItemRoleService;
    @Autowired
    protected InstallItemService installItemService;

    /**
     * Used to archive an item and remove all metadata related to the workflow.
     *
     * @param context the current application context.
     * @param wfi the workflow item to archive.
     * @throws SQLException if any database exception occurred
     * @throws AuthorizeException if any authorization occurred
     */
    protected void archive(Context context, XmlWorkflowItem wfi) throws SQLException, AuthorizeException {
        Item item = wfi.getItem();
        workflowItemRoleService.deleteForWorkflowItem(context, wfi);
        installItemService.installItem(context, wfi);
        itemService.clearMetadata(
            context, item, WorkflowRequirementsService.WORKFLOW_SCHEMA,
            Item.ANY, Item.ANY, Item.ANY
        );
        itemService.update(context, item);
    }

    /**
     * Add provenance information to the item using a custom message.
     *
     * @param context The current DSpace context.
     * @param item The item to which the provenance information will be added to.
     * @param message The custom message to be added to the provenance information.
     * @throws SQLException if any database exception occurred
     * @throws AuthorizeException if any authorization occurred
     */
    protected void addProvenance(Context context, Item item, String message) throws SQLException, AuthorizeException {
        String now = DCDate.getCurrent().toString();
        String userName = xmlWorkflowService
                .getEPersonName(context.getCurrentUser());
        String provDescription = getProvenanceStartId() + " " + message + " " + userName + " on " + now + " (GMT) ";
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(
            context,
            item,
            MetadataSchemaEnum.DC.getName(),
            "description", "provenance", "en",
            provDescription
        );
        itemService.update(context, item);
        context.restoreAuthSystemState();
    }

    protected void addValidationDate(Context context, Item item) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(
            context, item,
            MetadataSchemaEnum.DC.getName(), "date", "validated", null,
            DCDate.getCurrent().toString()
        );
        itemService.update(context, item);
        context.restoreAuthSystemState();
    }

    /**
     * Take a bitstream and restricts the access to the administrator group only.
     *
     * @param context The current DSpace context.
     * @param bitstream The bitstream to restrict.
     * @param adminGroup: The administrator group to grant read rights to.
     * @throws SQLException if any database exception occurred
     * @throws AuthorizeException if any authorization occurred
     */
    protected void restrictBitstream(Context context, Bitstream bitstream, Group adminGroup)
            throws SQLException, AuthorizeException {
        authorizeService.removeAllPolicies(context, bitstream);
        authorizeService.createResourcePolicy(
            context,
            bitstream,
            adminGroup,
            null,
            Constants.READ,
            ResourcePolicy.TYPE_CUSTOM,
            UCLouvainAccessStatusHelper.ADMINISTRATOR,
            null, null, null
        );
    }

    /**
     * Clear the active request change metadata field of the item.
     * 
     * @param context The current DSpace application context.
     * @param wfi The workflow item whose associated item will have its active request change metadata field cleared.
     */
    protected void clearRequestField(Context context, XmlWorkflowItem wfi) {
        Item item = wfi.getItem();
        try {
            // Retrieve the value of the active request field
            String value = itemService.getMetadataFirstValue(item, activeRF, Item.ANY);
            if (value != null) {
                this.itemService.clearMetadata(
                    context,
                    item,
                    activeRF.getSchema(),
                    activeRF.getElement(),
                    activeRF.getQualifier(),
                    Item.ANY
                );
            }
            return;
        } catch (Exception e) {
            logger.error("An error occurred while clearing the active request change metadata field.", e);
            return;
        }
    }
}
