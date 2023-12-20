package org.dspace.uclouvain.pdfAttestationGenerator.xmlworkflow.actions;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.pdfAttestationGenerator.factory.PDFAttestationGeneratorFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.eperson.EPerson;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;


/**
* Main action to generate a PDF attestation for the workflow item if his type is handled
*/
public class GeneratePDFAction extends ProcessingAction {
    
    @Autowired
    ItemService itemService;

    @Autowired
    MetadataUtils metadataUtils;

    private String source = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir");
    private String mailSubject = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("uclouvain.pdf_attestation.mail.subject");
    private String mailErrorSubject = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("uclouvain.pdf_attestation.mail.error.subject");

    @Override
    public void activate(Context c, XmlWorkflowItem wf){}

    /**
    * Create an email with some information from the submission, attach the PDF attestation and send it to the submitter
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
                EPerson submitter = dspaceItem.getSubmitter();
                HashMap<String, List<String>> map = MetadataUtils.getValuesHashMap(dspaceItem.getMetadata());

                try {
                    String templatePath = this.source + "/config/emails/pdf_attestation";
                    InputStream pdfAttestation = handler.getAttestationAsInputStream(uuid);

                    // Generate email
                    Email email = Email.getEmail(templatePath);
                    // TODO: Use submitter's email
                    // email.addRecipient(submitter.getEmail());
                    email.addRecipient("michael.pourbaix@uclouvain.be");
                    email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
                    email.addArgument(map.get("dspace_entity_type").get(0).toLowerCase());
                    email.addArgument(generateSubmissionResume(map));
                    email.setSubject(this.mailSubject);

                    email.addAttachment(pdfAttestation, map.get("dspace_entity_type").get(0) + "SubmissionAttestation.pdf", "application/pdf");
                    email.send();
                } catch (Exception e ) {
                    // Send an error email if something goes wrong
                    String templatePath = this.source + "/config/emails/pdf_attestation_error";

                    Email email = Email.getEmail(templatePath);
                    // TODO: Use submitter's email
                    // email.addRecipient(submitter.getEmail());
                    email.addRecipient("michael.pourbaix@uclouvain.be");
                    email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
                    email.addArgument(submitter.getFullName());
                    email.addArgument(map.get("dspace_entity_type").get(0).toLowerCase());
                    email.addArgument(generateSubmissionResume(map));
                    email.setSubject(this.mailErrorSubject);

                    email.send();

                    throw new RuntimeException(e);
                }   
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<String>();
    }

    /**
     * Generates a resume for the given submission containing the title, the abstract and authors
     * 
     * @param map The HashMap containing information about the submission
     * @return The resume as a String 
    */
    private static String generateSubmissionResume(HashMap<String, List<String>> map) {
        String title = map.get("dc_title").get(0);
        List<String> authors = map.get("dc_contributor_author");
        String abstractText = map.get("dc_description_abstract").get(0);
        return "Title: " + title +"\nAuthor(s): " + authors.stream().reduce("", (subtotal, element) -> subtotal + element + "; ") +"\nAbstract: " + abstractText;
    }
}
