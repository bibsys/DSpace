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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Custom review action for dissertation workflow.
 * The manager can:
 * - accept the dissertation so it can be archived.
 * - delete the dissertation which fully removes the item.
 * - edit the dissertation.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainDissertationReviewAction extends ReviewAction {

    private static final String SUBMIT_DELETE = "submit_confirm_delete";

    private Logger logger = LogManager.getLogger(UCLouvainDissertationReviewAction.class);

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        if (super.isOptionInParam(request)) {
            switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
                case SUBMIT_APPROVE:
                    return super.processAccept(c, wfi);
                case SUBMIT_DELETE:
                    return processDelete(c, wfi);
                default:
                    return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    private ActionResult processDelete(Context context, XmlWorkflowItem wfi) {
        // Delete the item (workflow and workspace) completely.
        // We need to delete both WorkflowItem and WorkspaceItem (and maybe item as well ??).
        try {
            XmlWorkflowServiceFactory.getInstance()
                .getXmlWorkflowService()
                .deleteWorkflowByWorkflowItem(context, wfi, context.getCurrentUser());
        } catch (Exception e) {
            logger.error(
                String.format(
                    "Manager %s could not delete workflowItem with id %s",
                    context.getCurrentUser().getEmail(), wfi.getID()
                )
            );
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }
        return new ActionResult(ActionResult.TYPE.TYPE_SUBMISSION_PAGE);
    }

    /**
     * Override the getOptions() method to only allow for validation and edit actions.
     */
    @Override
    public List<String> getOptions() {
        List<String> options = new ArrayList<>();
        options.add(SUBMIT_APPROVE);
        options.add(SUBMIT_DELETE);
        options.add(RETURN_TO_POOL);
        // Edit item button
        options.add(ProcessingAction.SUBMIT_EDIT_METADATA);
        return options;
    }
}
