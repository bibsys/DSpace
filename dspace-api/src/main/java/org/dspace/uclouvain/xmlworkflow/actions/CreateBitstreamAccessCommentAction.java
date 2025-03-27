/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.actions;

import static org.dspace.core.Constants.CONTENT_BUNDLE_NAME;
import static org.dspace.uclouvain.constants.AccessConditions.EMBARGO;
import static org.dspace.uclouvain.constants.AccessConditions.OPEN_ACCESS;

import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.service.CommentService;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.dspace.workflow.WorkflowException;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Workflow action used to store initial bitstream access condition as a {@link org.dspace.uclouvain.content.Comment}
 *   For each bitstream associated to the item a new comment will be created including the initial access condition to
 *   this bitstream.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class CreateBitstreamAccessCommentAction extends ProcessingAction {

    private Logger logger = LogManager.getLogger(CreateBitstreamAccessCommentAction.class);

    @Autowired
    private CommentService commentService;
    @Autowired
    private UCLouvainResourcePolicyService policyService;

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {}

    @Override
    public ActionResult execute(
        Context context,
        XmlWorkflowItem wfi,
        Step step,
        HttpServletRequest request
    ) throws SQLException, AuthorizeException, IOException, WorkflowException {
        Item item = wfi.getItem();
        if (item != null) {
            item.getBundles(CONTENT_BUNDLE_NAME).stream()
                .flatMap(bundle -> bundle.getBitstreams().stream())
                .forEach(bitstream -> this.createInitialAccessBitstreamComment(context, item, bitstream));
        }
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        return null;
    }

    private void createInitialAccessBitstreamComment(Context context, Item item, Bitstream bitstream) {
        try {
            ResourcePolicy masterPolicy = policyService.getMasterPolicy(policyService.find(context, bitstream));
            String accessStatus = (masterPolicy != null) ? masterPolicy.getRpName() : OPEN_ACCESS;
            if (masterPolicy != null && masterPolicy.getRpName().equalsIgnoreCase(EMBARGO)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                accessStatus += " until (" + dateFormat.format(masterPolicy.getStartDate()) + ")";
            }

            String commentContent = "Bitstream@" + bitstream.getID() + " with name \"" + bitstream.getName() + "\" :: ";
            commentContent += "Initial accessCondition is " + accessStatus + "\n";
            commentService.create(context, item, context.getCurrentUser(), commentContent);
        } catch (Exception e) {
            logger.warn("Error generating comment :: " + e.getMessage(), e);
        }
    }


}
