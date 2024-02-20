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

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.util.Util;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.service.InstallItemService;
import org.dspace.core.Context;
import org.dspace.xmlworkflow.service.WorkflowRequirementsService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.WorkflowItemRoleService;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Custom review action for master theses.
 * (mainly a copy of @org.dspace.xmlworkflow.state.actions.processingaction.ReviewAction).
 * This Action contains only two outputs: Accepted or Rejected
 * In the case 'Accepted', we continue the workflow
 * In the case 'Rejected', we change the state of the workflow item to 'Withdrawn'
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 * @version $Revision$
 */
public class UCLouvainThesisReviewAction extends ReviewAction {

    private static final String SUBMITTER_IS_DELETED_PAGE = "submitter_deleted";

    @Autowired
    protected WorkflowItemRoleService workflowItemRoleService;
    @Autowired
    protected InstallItemService installItemService;

    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request)
        throws SQLException, AuthorizeException, IOException {
        if (super.isOptionInParam(request)) {
            switch (Util.getSubmitButton(request, SUBMIT_CANCEL)) {
                case SUBMIT_APPROVE:
                    return processAccept(c, wfi);
                case SUBMIT_REJECT:
                    return this.processRejectPage(c, wfi, request);
                case SUBMITTER_IS_DELETED_PAGE:
                    return processSubmitterIsDeletedPage(c, wfi, request);
                default:
                    return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
            }
        }
        return new ActionResult(ActionResult.TYPE.TYPE_CANCEL);
    }

    /**
     * Process result when option {@link this#SUBMIT_REJECT} is selected.
     * - Sets the reason and workflow step responsible on item in dc.description.provenance
     * - Send workflow back to the submission
     * If reason is not given => error
     */
    @Override
    public ActionResult processRejectPage(Context c, XmlWorkflowItem wfi, HttpServletRequest request)
            throws SQLException, AuthorizeException, IOException {
        String reason = request.getParameter(REJECT_REASON);
        if (reason == null || reason.isBlank()) {
            addErrorField(request, REJECT_REASON);
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }
        c.turnOffAuthorisationSystem();
        // Archive the item, then instantly withdraw it
        Item archivedItem = this.archive(c, wfi);
        itemService.withdraw(c, archivedItem);
        c.restoreAuthSystemState();
        return new ActionResult(ActionResult.TYPE.TYPE_PAGE);
    }

    private Item archive(Context context, XmlWorkflowItem wfi) throws SQLException, AuthorizeException {
        Item item = wfi.getItem();
        workflowItemRoleService.deleteForWorkflowItem(context, wfi);
        installItemService.installItem(context, wfi);
        itemService.clearMetadata(
                context,
                item,
                WorkflowRequirementsService.WORKFLOW_SCHEMA,
                Item.ANY,
                Item.ANY,
                Item.ANY
        );
        itemService.update(context, item);
        return item;
    }
}
