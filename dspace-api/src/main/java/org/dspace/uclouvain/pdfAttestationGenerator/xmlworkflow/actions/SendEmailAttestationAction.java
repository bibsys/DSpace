/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pdfAttestationGenerator.xmlworkflow.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.mails.ThesisAuthorAttestationEmail;
import org.dspace.uclouvain.core.mails.ThesisErrorAttestationEmail;
import org.dspace.uclouvain.core.mails.ThesisSupervisorAttestationEmail;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
* Main action to generate a PDF attestation for the workflow item if his type is handled
*/
public class SendEmailAttestationAction extends ProcessingAction {

    private final Logger logger = LogManager.getLogger(SendEmailAttestationAction.class);

    @Autowired
    private ItemService itemService;

    // FIELD CONFIGURATION
    private final String authorEmailField;
    private final String supervisorEmailField;

    public SendEmailAttestationAction() {
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        authorEmailField = new MetadataField(configService.getProperty(
                "uclouvain.global.metadata.authoremail.field", "authors.email")).getFullString("_");
        supervisorEmailField = new MetadataField(configService.getProperty(
                "uclouvain.global.metadata.advisoremail.field", "advisors.email")).getFullString("_");
    }

    @Override
    public void activate(Context context, XmlWorkflowItem wf){}

    /**
    * Create an email with information from the submission, attach the PDF attestation and send it to the submitter.
    * Action used by the workflow system to send an email when a submission is made.
    */
    @Override
    public ActionResult execute(Context context, XmlWorkflowItem wfi, Step step, HttpServletRequest request) {
        Item dspaceItem = null;
        // In any cases, the email attestation generation must not be blocking. We always return a valid response.
        ActionResult staticResponse = new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        // UUID of the current workflow item
        UUID uuid = wfi.getItem().getID();
        try {
            dspaceItem = itemService.find(context, uuid);
            HashMap<String, List<String>> map = MetadataUtils.getValuesHashMap(dspaceItem.getMetadata());
            // Checks if authors and promoter are present
            if (map.get(authorEmailField) == null || map.get(supervisorEmailField) == null) {
                logger.warn("No authors or supervisors found for the following item: " + dspaceItem.getID()
                    + "--> Aborting email attestation generation.");
                return staticResponse;
            }

            // Send email to authors
            new ThesisAuthorAttestationEmail(context, dspaceItem).sendEmail();

            // Send email to promoters
            new ThesisSupervisorAttestationEmail(context, dspaceItem).sendEmail();
        } catch (Exception e) {
            logger.error("[" + uuid + "] Exception occurred sending attestation email: " + e.getMessage(), e);
            if (dspaceItem != null) {
                try {
                    // try to send an error email to the author.
                    new ThesisErrorAttestationEmail(context, dspaceItem, e).sendEmail();
                } catch (Exception ignored) {
                    // do nothing
                }
            }
        }
        return staticResponse;
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<>();
    }
}