/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.actions;

import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.mails.PublicationNotifyAuthorsEmail;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Action to notify all the authors of a publication when it is deposited.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class NotifyAuthorsAction extends ProcessingAction {
    protected static final Logger logger = LogManager.getLogger(NotifyAuthorsAction.class);

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {}

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem wfi, Step step, HttpServletRequest request) {
        final ActionResult result = new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        // 1. Retrieve the item.
        // 2. If item is not null, send an email to all the authors.
        Item item = wfi.getItem();
        if (item != null) {
            try {
                new PublicationNotifyAuthorsEmail(context, item).sendEmail();
            } catch (Exception e) {
                logger.error(
                    "Could not build or send the publication notification email",
                    e
                );
            }
        }
        return result;
    }

    @Override
    public List<String> getOptions() {
        // No options for this action so return an empty list.
        return Collections.emptyList();
    }
}
