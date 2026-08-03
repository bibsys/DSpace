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

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.service.CommentService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Custom edit action for master theses.
 * This action only has one output of 'approve' and does not allow to reject the item.
 * The user can also edit the item metadata before approving it.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainThesisEditAction extends UCLouvainThesisAction {
    private static final String SUBMITTER_IS_DELETED_PAGE = "submitter_deleted";
    private static final String SUBMIT_WITHDRAW_REJECT = "submit_withdraw_reject";
    private static final String RETURN_TO_MANAGER = "submit_return_to_manager";

    @Autowired
    private CommentService commentService;

    private Logger logger = LogManager.getLogger(UCLouvainThesisEditAction.class);

    /**
     * Method executed to map each option to a specific action.
     * DEV_NOTE: The option is extracted from the incoming request by splitting the submit button name.
     */
    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        if (super.isOptionInParam(request)) {
            switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
                case SUBMIT_APPROVE:
                    return processAccept(c, wfi);
                case SUBMIT_WITHDRAW_REJECT:
                    return processRejectPage(c, wfi, request);
                case SUBMITTER_IS_DELETED_PAGE:
                    return processSubmitterIsDeletedPage(c, wfi, request);
                case RETURN_TO_MANAGER:
                    return processReturnToManager(c, wfi, request);
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
        // In this case approve means "enter archive"
        options.add(SUBMIT_APPROVE);
        // Reject the submission, sets it to withdrawn.
        options.add(SUBMIT_WITHDRAW_REJECT);
        // Return the item to the previous workflow step.
        options.add(RETURN_TO_MANAGER);
        // Return to pool for re-assignment to another editor.
        options.add(RETURN_TO_POOL);
        // Edit item button
        options.add(ProcessingAction.SUBMIT_EDIT_METADATA);
        return options;
    }

    /**
     * Process result when option 'SUBMIT_WITHDRAW_REJECT' is selected:
     * - First archive the item.
     * - Once archived, withdrawn it, to be only visible by administrators.
     *
     * @param context the current application context.
     * @param wfi the workflow item that is being operated.
     * @param request the current request object.
     * @return An ActionResult object which represents the output of the action.
     * @throws SQLException if any database exception occurred
     * @throws AuthorizeException if any authorization occurred
     */
    @Override
    public ActionResult processRejectPage(Context context, XmlWorkflowItem wfi, HttpServletRequest request)
            throws SQLException, AuthorizeException {
        Item item = wfi.getItem();
        addValidationDate(context, item);
        addProvenance(context, item, "Rejected for entry into archive and placed into withdrawn state by librarian: '" +
                context.getCurrentUser().getEmail() + "'");
        context.turnOffAuthorisationSystem();
        // Archive the item, then instantly withdraw it
        archive(context, wfi);
        itemService.withdraw(context, item);
        context.restoreAuthSystemState();
        return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
    }

    /**
     * Process the action 'RETURN_TO_MANAGER' which can be performed by a manager and will:
     *  1) Send the item back to the manager for modifications.
     *  2) Add a message (given by the librarian) into a metadata field of the item.
     *  3) The message will then be used to inform the manager of the necessary changes.
     *
     * @param context The current DSpace context.
     * @param wfi The workflow item that is being operated.
     * @param request The current request object.
     * @return An ActionResult object which represents the output of the action.
     */
    public ActionResult processReturnToManager(Context context, XmlWorkflowItem wfi, HttpServletRequest request) {
        try {
            context.turnOffAuthorisationSystem();

            // Get the mandatory reason from the request object
            String reason = request.getParameter("reason");
            if (StringUtils.isEmpty(reason)) {
                return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
            }

            // Send the item back to manager for additional validation.
            xmlWorkflowService.sendWorkflowItemToPreviousStep(
                context,
                wfi,
                context.getCurrentUser(),
                "",
                reason
            );

            // Encode the reason in the metadata field & store this reason as a new comment related to the item.
            Item item = wfi.getItem();
            if (item != null) {
                itemService.setMetadataSingleValue(context, item, activeRF, null, reason);
                String commentContent = "Send back to manager for modifications :: " + reason;
                commentService.create(context, item, context.getCurrentUser(), commentContent);
                // Send email to manager to notify for the change request.
                // TODO: What to do here ????
                // new ThesisChangeRequestEmail(context, item).sendEmail();
            }
            context.restoreAuthSystemState();
            return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
        } catch (Exception e) {
            logger.error("Error while returning the item to the submitter: " + e.getMessage(), e);
            return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
        }
    }
}
