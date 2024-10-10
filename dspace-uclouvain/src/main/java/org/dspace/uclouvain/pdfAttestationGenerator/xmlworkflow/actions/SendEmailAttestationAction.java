package org.dspace.uclouvain.pdfAttestationGenerator.xmlworkflow.actions;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.dspace.uclouvain.core.mails.ThesisAuthorAttestationEmail;
import org.dspace.uclouvain.core.mails.ThesisErrorAttestationEmail;
import org.dspace.uclouvain.core.mails.ThesisPromoterAttestationEmail;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.HandlerNotFoundException;
import org.dspace.uclouvain.pdfAttestationGenerator.factory.PDFAttestationGeneratorFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;


/**
* Main action to generate a PDF attestation for the workflow item if his type is handled
*/
public class SendEmailAttestationAction extends ProcessingAction {
    
    @Autowired
    ItemService itemService;

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private String algorithm = this.configurationService.getProperty("uclouvain.api.bitstream.download.algorithm", "MD5");
    private String encryptionKey = this.configurationService.getProperty("uclouvain.api.bitstream.download.secret", "");

    private Logger logger = LogManager.getLogger(SendEmailAttestationAction.class);

    // FIELD CONFIGURATION
    private String authorEmailField;
    private String promoterEmailField;

    public SendEmailAttestationAction() {
        // Instantiate the metadata fields from the configuration
        this.authorEmailField = new MetadataField(
            this.configurationService.getProperty("uclouvain.global.metadata.authoremail.field", "authors.email")
        ).getFullString("_");
        this.promoterEmailField = new MetadataField(
            this.configurationService.getProperty("uclouvain.global.metadata.advisoremail.field", "advisors.email")
        ).getFullString("_");
    }

    @Override
    public void activate(Context c, XmlWorkflowItem wf){}

    /**
    * Create an email with some information from the submission, attach the PDF attestation and send it to the submitter.
    * Action used by the workflow system to send an email when a submission is made.
    */
    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request){
        // UUID of the current workflow item
        UUID uuid = wfi.getItem().getID();
        try {
            // Recover the correct handler for this submission
            PDFAttestationGeneratorHandler handler = PDFAttestationGeneratorFactory.getInstance().getHandlerInstance(uuid);
            // If the type of the submission is supported, we have an handler
            if (handler != null) {

                Item dspaceItem = itemService.find(c, uuid);
                HashMap<String, List<String>> map = MetadataUtils.getValuesHashMap(dspaceItem.getMetadata());

                // Checks if authors and promoter are present
                if (map.get(this.authorEmailField) == null || map.get(this.promoterEmailField) == null) {
                    logger.warn("No authors or advisors found for the following item: " + dspaceItem.getID() + ". Aborting email attestation generation.");
                    return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
                }

                try {
                    // We need to use a `ByteArrayInputStream` in order to be able to reset the stream after sending the email to the submitter(s).
                    ByteArrayInputStream pdfAttestation = new ByteArrayInputStream(
                        IOUtils.toByteArray(handler.getAttestationAsInputStream(uuid))
                    );

                    // Mark the position to reset to
                    pdfAttestation.mark(pdfAttestation.available());

                    // Send an email to authors 
                    new ThesisAuthorAttestationEmail(dspaceItem, pdfAttestation).sendEmail();
                    
                    // Reset to the previously marked position. We need to do that because the stream has been consumed by the previous email.
                    pdfAttestation.reset();

                    // Send an email to promoters
                    new ThesisPromoterAttestationEmail(dspaceItem, pdfAttestation, this.algorithm, this.encryptionKey).sendEmail();
                } catch (Exception e) {
                    // Send an error email if something goes wrong
                    logger.error("An exception occurred while generating email attestation for uuid: " + uuid + ": " + e.getMessage());
                    try {
                        new ThesisErrorAttestationEmail(dspaceItem, e).sendEmail();
                    } catch (Exception errorException) {
                        logger.error("Could not generate or send the error email for email attestation for uuid: " + uuid + ": " + errorException.getMessage());
                    }
                }   
            }
        } catch (HandlerNotFoundException e) {
            logger.error("No handler found for item with uuid: " + uuid + ": " + e.getMessage());
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        } catch (Exception e) {
            logger.error("An exception occurred while generating email attestation for uuid: " + uuid + ": " + e.getMessage());
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<String>();
    }
}
