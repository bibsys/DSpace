/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.actions;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.mails.ThesisChangeRequestEmail;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.plugins.UCLouvainAccessStatusHelper;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Custom review action for master theses.
 * This Action contains three outputs: 'Accepted', 'Accepted without diffusion' and 'Rejected'.
 * In the case 'Accepted', we continue the workflow;
 * In the case 'Accepted without diffusion', same as 'Accepted' but we restrict bitstream && add a message to the
 * metadata;
 * In the case 'Rejected', we change the state of the workflow item to 'Withdrawn' && we add it to archive;
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @version $Revision$
 */
public class UCLouvainThesisReviewAction extends ReviewAction {

    private Logger logger = LogManager.getLogger(UCLouvainThesisReviewAction.class);

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final String SUBMITTER_IS_DELETED_PAGE = "submitter_deleted";
    private static final String SUBMIT_APPROVE = "submit_confirm_approve";
    private static final String SUBMIT_APPROVE_WITHOUT_DIFFUSION = "submit_approve_no_diffusion";
    private static final String SUBMIT_WITHDRAW_REJECT = "submit_withdraw_reject";
    private static final String RETURN_TO_SUBMITTER = "submit_return_to_submitter";

    private MetadataField activeRequestField = new MetadataField(configService
            .getProperty("uclouvain.global.metadata.activerequestchange.field"));

    @Autowired(required = true)
    protected WorkflowItemRoleService workflowItemRoleService;
    @Autowired(required = true)
    protected InstallItemService installItemService;
    @Autowired
    private AuthorizeService authorizeService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private BitstreamService bitstreamService;

    /**
     * Method executed to map each option to a specific action.
     * The option is extracted form the incoming request by splitting the submit button name.
     */
    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        if (super.isOptionInParam(request)) {
            switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
                case SUBMIT_APPROVE:
                    return processAccept(c, wfi);
                case SUBMIT_APPROVE_WITHOUT_DIFFUSION:
                    return processAcceptWithoutDiffusion(c, wfi);
                case SUBMIT_WITHDRAW_REJECT:
                    return processRejectPage(c, wfi, request);
                case SUBMITTER_IS_DELETED_PAGE:
                    return processSubmitterIsDeletedPage(c, wfi, request);
                case RETURN_TO_SUBMITTER:
                    return processReturnToSubmitter(c, wfi, request);
                default:
                    return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    /**
     * Get the list of options available for this action.
     * This is also used for rendering buttons on the frontend.
     */
    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_APPROVE);
        options.add(SUBMIT_APPROVE_WITHOUT_DIFFUSION);
        options.add(SUBMIT_WITHDRAW_REJECT);
        options.add(RETURN_TO_SUBMITTER);
        options.add(RETURN_TO_POOL);
        // Edit item button
        options.add(ProcessingAction.SUBMIT_EDIT_METADATA);
        return options;
    }

    public ActionResult processAccept(Context ctx, XmlWorkflowItem wfi)
            throws SQLException, AuthorizeException {
        Item currentItem = wfi.getItem();
        addValidationDate(ctx, currentItem);
        return super.processAccept(ctx, wfi);
    }

    /**
     * Process result when option 'SUBMIT_APPROVE_WITHOUT_DIFFUSION' is selected.
     * - First delete all bitstream restrictions and add only administrator access.
     * - Add a new tag to keep a trace of the operation in the 'dc.description.X' metadata field.
     * @param ctx the application context
     * @param wfi the workflow item to manage
     * @return the action to perform to continue item life cycle
     */
    public ActionResult processAcceptWithoutDiffusion(Context ctx, XmlWorkflowItem wfi)
            throws SQLException, AuthorizeException {
        // When a manager chooses to approve a publication without any diffusion, we need:
        //   1) Change bitstreams access restriction: force "admin only" restriction for all bitstreams from ORIGINAL
        //      bundle
        //   2) add the "dc.date.validated" metadata
        //   3) add the "dc.description.provenance" to store item history
        Item currentItem = wfi.getItem();
        Group adminGroup = groupService.findByName(ctx, "Administrator");
        if (adminGroup != null) {
            for (Bitstream bitstream: bitstreamService.getBitstreamByBundleName(currentItem, "ORIGINAL")) {
                this.restrictBitstream(ctx, bitstream, adminGroup);
            }
        }
        addValidationDate(ctx, currentItem);
        addProvenance(ctx, currentItem,
                "Approved with no diffusion for entry into archive by user: '" + ctx.getCurrentUser().getEmail() + "'");
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    /**
     * Process result when option 'SUBMIT_WITHDRAW_REJECT' is selected:
     * - First archive the item.
     * - Once archived, withdrawn it, to be only visible by administrators.
     * 
     * @param ctx the current application context.
     * @param wfi the workflow item that is being operated.
     * @param request the current request object.
     * @return An ActionResult object which represents the output of the action.
     * @throws SQLException if any database exception occurred
     * @throws AuthorizeException if any authorization occurred
     */
    @Override
    public ActionResult processRejectPage(Context ctx, XmlWorkflowItem wfi, HttpServletRequest request)
            throws SQLException, AuthorizeException {
        Item item = wfi.getItem();
        addValidationDate(ctx, item);
        addProvenance(ctx, item, "Rejected for entry into archive and placed into withdrawn state by user: '" +
            ctx.getCurrentUser().getEmail() + "'");
        ctx.turnOffAuthorisationSystem();
        // Archive the item, then instantly withdraw it
        archive(ctx, wfi);
        itemService.withdraw(ctx, item);
        ctx.restoreAuthSystemState();
        return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
    }

    /**
     * Process the action 'RETURN_TO_SUBMITTER' which can be performed by a manager and will:
     *  -> Send the item back to the submitter for modifications.
     *  -> Add a message (given by the manager) into a metadata field of the item.
     *  -> The message will then be used to inform the submitter of the needed changes.
     * 
     * @param context  The current DSpace context.
     * @param wfi      The workflow item that is being operated.
     * @param request  The current request object.
     * @return An ActionResult object which represents the output of the action.
     */
    public ActionResult processReturnToSubmitter(Context context, XmlWorkflowItem wfi, HttpServletRequest request) {
        try {
            context.turnOffAuthorisationSystem();
            // Send the item back to submission state.
            xmlWorkflowService.sendWorkflowItemBackSubmission(
                    context, wfi, context.getCurrentUser(), "", "Send back to submitter for modifications");
            // Get the mandatory reason from the request object
            String reason = request.getParameter("reason");
            if (StringUtils.isEmpty(reason)) {
                return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
            }
            // Encode the reason in the metadata field
            itemService.setMetadataSingleValue(context, wfi.getItem(), activeRequestField, null, reason);
            // Send email to submitter to notify for the change request.
            new ThesisChangeRequestEmail(context, wfi.getItem(), reason).sendEmail();
            context.restoreAuthSystemState();
            return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
        } catch (Exception e) {
            this.logger.error("Error while returning the item to the submitter: " + e.getMessage());
            return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        }
    }

    /**
     * Used to archive an item and remove all metadata related to the workflow.
     * @param ctx the current application context.
     * @param wfi the workflow item to archive.
     * @throws SQLException if any database exception occurred
     * @throws AuthorizeException if any authorization occurred
     */
    private void archive(Context ctx, XmlWorkflowItem wfi) throws SQLException, AuthorizeException {
        Item item = wfi.getItem();
        workflowItemRoleService.deleteForWorkflowItem(ctx, wfi);
        installItemService.installItem(ctx, wfi);
        itemService.clearMetadata(ctx, item, WorkflowRequirementsService.WORKFLOW_SCHEMA, Item.ANY, Item.ANY, Item.ANY);
        itemService.update(ctx, item);
    }

    /**
     * Take a bitstream and restricts the access to the administrator group only.
     * @param ctx        The current DSpace context.
     * @param bitstream  The bitstream to restrict.
     * @param adminGroup The administrator group to grant read rights to.
     * @throws SQLException if any database exception occurred
     * @throws AuthorizeException if any authorization occurred
     */
    private void restrictBitstream(Context ctx, Bitstream bitstream, Group adminGroup)
            throws SQLException, AuthorizeException {
        authorizeService.removeAllPolicies(ctx, bitstream);
        authorizeService.createResourcePolicy(
                ctx,
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
     * Add provenance information to the item using a custom message.
     * @param context The current DSpace context.
     * @param item    The item to which the provenance information will be added to.
     * @param message The custom message to be added to the provenance information.
     * @throws SQLException if any database exception occurred
     * @throws AuthorizeException if any authorization occurred
     */
    private void addProvenance(Context context, Item item, String message) throws SQLException, AuthorizeException {
        String now = DCDate.getCurrent().toString();
        String userName = xmlWorkflowService.getEPersonName(context.getCurrentUser());
        String provDescription = getProvenanceStartId() + " " + message + " " + userName + " on " + now + " (GMT) ";

        context.turnOffAuthorisationSystem();
        this.itemService.addMetadata(
                context,
                item,
                MetadataSchemaEnum.DC.getName(), "description", "provenance",
                "en",
                provDescription
        );
        this.itemService.update(context, item);
        context.restoreAuthSystemState();
    }

    private void addValidationDate(Context context, Item item) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(
                context, item,
                MetadataSchemaEnum.DC.getName(), "date", "validated", null,
                DCDate.getCurrent().toString()
        );
        itemService.update(context, item);
        context.restoreAuthSystemState();
    }
}
