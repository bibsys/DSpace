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
import org.dspace.uclouvain.core.mails.DissertationDepositEmail;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

/**
 * Action to notify all the authors of a publication item.
 * The email to send changes base on the publication type of the item.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class PublicationNotifyDepositAction extends ProcessingAction {
    protected static final Logger logger = LogManager.getLogger(PublicationNotifyDepositAction.class);
    @Override
    public void activate(Context c, XmlWorkflowItem wf) {}

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem wfi, Step step, HttpServletRequest request) {
        final ActionResult result = new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        // Retrieve the item.
        Item item = wfi.getItem();
        // If item is not null and of type publication, send a specific email depending on its publication type.
        if (item != null && "Publication".equals(itemService.getEntityType(item))) {
            Publication publication;
            try {
                publication = PublicationFactory.build(item);
            } catch (InvalidModelEntityTypeException e) {
                return result;
            }
            try {
                switch (publication.getMainType()) {
                    case "text::thesis":
                        new DissertationDepositEmail(context, item).sendEmail();
                        break;
                    default:
                        logger.warn(
                            "Reached the notifyAuthorsAction with an non-processable publication type "
                            + "'" + publication.getMainType() + "', UUID: " + item.getID()
                        );
                }
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
