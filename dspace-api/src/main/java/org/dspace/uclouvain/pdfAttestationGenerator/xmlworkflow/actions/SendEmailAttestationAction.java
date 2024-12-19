/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pdfAttestationGenerator.xmlworkflow.actions;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.mails.ThesisAuthorAttestationEmail;
import org.dspace.uclouvain.core.mails.ThesisErrorAttestationEmail;
import org.dspace.uclouvain.core.mails.ThesisPromoterAttestationEmail;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.HandlerNotFoundException;
import org.dspace.uclouvain.pdfAttestationGenerator.factory.PDFAttestationGeneratorFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;
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
    ItemService itemService;

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final String algorithm = configService.getProperty("uclouvain.api.bitstream.download.algorithm", "MD5");
    private final String encryptionKey = configService.getProperty("uclouvain.api.bitstream.download.secret", "");
    private final String authorEmailField;
    private final String promoterEmailField;

    public SendEmailAttestationAction() {
        authorEmailField = new MetadataField(configService.getProperty(
                "uclouvain.global.metadata.authoremail.field", "authors.email")).getFullString("_");
        promoterEmailField = new MetadataField(configService.getProperty(
                "uclouvain.global.metadata.advisoremail.field", "advisors.email")).getFullString("_");
    }

    @Override
    public void activate(Context context, XmlWorkflowItem wf){}

    /**
    * Create an email with information from the submission, attach the PDF attestation and send it to the submitter.
    * Action used by the workflow system to send an email when a submission is made.
    */
    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) {
        Item dspaceItem = null;
        // In any cases, the email attestation generation must not be blocking. We always return a valid response.
        ActionResult staticResponse = new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        // UUID of the current workflow item
        UUID uuid = wfi.getItem().getID();
        try {
            // Recover the correct handler for this submission
            PDFAttestationGeneratorHandler handler = PDFAttestationGeneratorFactory
                .getInstance()
                .getHandlerInstance(uuid);
            dspaceItem = itemService.find(c, uuid);
                HashMap<String, List<String>> map = MetadataUtils.getValuesHashMap(dspaceItem.getMetadata());

                // Checks if authors and promoter are present
                if (map.get(authorEmailField) == null || map.get(promoterEmailField) == null) {
                logger.warn("No authors or supervisors found for the following item: " + dspaceItem.getID()
                        + "--> Aborting email attestation generation.");
                return staticResponse;
                }

            // We need to use a `ByteArrayInputStream` to be able to reset the stream after sending
            // the email to the submitter(s).
            ByteArrayInputStream pdfAttestation = new ByteArrayInputStream(
                IOUtils.toByteArray(handler.getAttestationAsInputStream(uuid))
            );
            // Mark the position to reset to
            pdfAttestation.mark(pdfAttestation.available());
            // Send email to authors
            new ThesisAuthorAttestationEmail(dspaceItem, pdfAttestation).sendEmail();
            // Reset to the previously marked position.
            // We need to do that because the stream has been consumed by the previous email.
            pdfAttestation.reset();
            // Send email to promoters
            new ThesisPromoterAttestationEmail(dspaceItem, pdfAttestation, algorithm, encryptionKey)
                    .sendEmail();
        } catch (HandlerNotFoundException e) {
            logger.error("[" + uuid + "] No handler found for item with uuid:" + e.getMessage());
        } catch (Exception e) {
            logger.error("[" + uuid + "] Exception occurred during email attestation generation: "
                            + e.getMessage());
            if (dspaceItem != null) {
                try {
                    new ThesisErrorAttestationEmail(dspaceItem, e).sendEmail();
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