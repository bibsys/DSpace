/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pdfAttestationGenerator.xmlworkflow.actions;

import static org.dspace.core.Constants.CONTENT_BUNDLE_NAME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BinaryOperator;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.directLink.DirectLinkGenerator;
import org.dspace.uclouvain.core.directLink.DirectLinkGeneratorFactory;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.ResumeGenerationException;
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

    private Logger logger = LogManager.getLogger(SendEmailAttestationAction.class);

    @Autowired
    private ItemService itemService;
    @Autowired
    private DirectLinkGeneratorFactory directLinkGeneratorFactory;

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final String source = configService.getProperty("dspace.dir");
    private final String mailSubject = configService.getProperty("uclouvain.pdf_attestation.mail.subject");
    private final String mailErrorSubject = configService.getProperty("uclouvain.pdf_attestation.mail.error.subject");
    private final List<String> recipientsConfig = Arrays.asList(
        configService.getArrayProperty("uclouvain.pdf_attestation.mail.recipients", new String[0]));

    @Override
    public void activate(Context c, XmlWorkflowItem wf){}

    /**
    * Create an email with some information from the submission, attach the PDF attestation and send it to the submitter
    */
    @Override
    public ActionResult execute(Context c, XmlWorkflowItem wfi, Step step, HttpServletRequest request) {
        // UUID of the current workflow item
        UUID uuid = wfi.getItem().getID();
        try {
            // Recover the correct handler for this submission
            PDFAttestationGeneratorHandler handler = PDFAttestationGeneratorFactory
                .getInstance()
                .getHandlerInstance(uuid);
            // If the type of the submission is supported, we have an handler
            if (handler != null) {
                Item dspaceItem = itemService.find(c, uuid);
                HashMap<String, List<String>> map = MetadataUtils.getValuesHashMap(dspaceItem.getMetadata());
                try {
                    String submitterTemplatePath = source + "/config/emails/pdf_attestation_author";
                    String promoterTemplatePath = source + "/config/emails/pdf_attestation_promoter";
                    // We need to use a `ByteArrayInputStream` to be able to reset the stream after sending the email
                    // to the submitter(s).
                    ByteArrayInputStream pdfAttestation = new ByteArrayInputStream(
                        IOUtils.toByteArray(handler.getAttestationAsInputStream(uuid))
                    );
                    // Mark the position to reset to
                    pdfAttestation.mark(pdfAttestation.available());
                    // Email submitters
                    sendSubmitterEmail(map, dspaceItem, submitterTemplatePath, pdfAttestation);
                    // Reset to the previously marked position. We need to do that because the stream has been consumed
                    // by the previous email.
                    pdfAttestation.reset();
                    // Email to promoters
                    sendPromoterEmail(map, dspaceItem, promoterTemplatePath, pdfAttestation);
                } catch (Exception e) {
                    // Send an error email if something goes wrong
                    logger.error("An exception occurred while generating email attestation for uuid: " + uuid + ": "
                            + e.getMessage());
                    try {
                        sendErrorEmail(map, dspaceItem.getSubmitter(), dspaceItem, e);
                    } catch (Exception ex) {
                        logger.error("Could not generate or send the error email for email attestation for uuid: "
                            + uuid + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("An exception occurred while generating email attestation for uuid: " + uuid + ": "
                + e.getMessage());
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
        }
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
    }

    @Override
    public List<String> getOptions() {
        return new ArrayList<>();
    }

    /**
     * Email all the submitters with the PDF attestation attached to it.
     *
     * @param metadata A HashMap containing all the metadata of the submission.
     * @param item The DSpace item corresponding to the submission.
     * @param templatePath The path to the email template to use.
     * @param attestation The PDF attestation to attach to the email.
     * @throws Exception
     */
    private void sendSubmitterEmail(
            HashMap<String, List<String>> metadata, Item item, String templatePath, InputStream attestation
    ) throws Exception {
        Email email = generateBaseEmail(metadata, templatePath, List.of("authors_email"), attestation);
        email.send();
    }

    /**
     * Email all the promoters with the PDF attestation attached to it.
     *
     * @param metadata A HashMap containing all the metadata of the submission.
     * @param item The DSpace item corresponding to the submission.
     * @param templatePath The path to the email template to use.
     * @param attestation The PDF attestation to attach to the email.
     * @throws Exception
     */
    private void sendPromoterEmail(
            HashMap<String, List<String>> metadata, Item item, String templatePath, InputStream attestation
    ) throws Exception {
        Email email = generateBaseEmail(metadata, templatePath, List.of("advisors_email"), attestation);
        appendUrlsToEmail(metadata, email, item);
        email.send();
    }

    /**
     * Send an error email to both the submitters and the promoters with the stacktrace of the exception.
     * This is done when an error is occurring when generating the PDF attestation (or an email).
     *
     * @param metadata A HashMap containing all the metadata of the submission.
     * @param submitter The EPerson who submitted the item.
     * @param item The DSpace item corresponding to the submission.
     * @param exception The exception that occurred.
     * @throws Exception
     */
    private void sendErrorEmail(
            HashMap<String, List<String>> metadata, EPerson submitter, Item item, Exception exception
    ) throws Exception {
        Email email = Email.getEmail(source + "/config/emails/pdf_attestation_error");
        addRecipients(metadata, Arrays.asList("authors_email", "advisors_email"), email);
        email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
        email.addArgument(submitter.getFullName());
        email.addArgument(metadata.get("dspace_entity_type").get(0).toLowerCase());
        email.addArgument("Here are some information that might be useful for our team:\n -> Item's uuid: "
                + item.getID() + "\n-> Stacktrace:\n" + ExceptionUtils.getStackTrace(exception));
        email.setSubject(mailErrorSubject);
        email.send();
    }

    /**
     * Generates a summary for the given submission containing the title, the abstract and the authors.
     * 
     * @param map The HashMap containing information about the submission.
     * @return The summary as a String.
    */
    private static String generateSubmissionResume(HashMap<String, List<String>> map) throws ResumeGenerationException {
        try {
            BinaryOperator<String> parser = (subtotal, element) -> subtotal + element + "; ";
            List<String> resultString = new ArrayList<>();
            // Retrieve all required metadata && check if they are existing before adding them to the submission's
            // summary.
            List<String> title = map.get("dc_title");
            if (title != null && !title.isEmpty()) {
                resultString.add("Title: " + title.get(0));
            }
            List<String> authors = map.get("dc_contributor_author");
            if (authors != null && !authors.isEmpty()) {
                resultString.add("Authors: " + authors.stream().reduce("", parser));
            }
            List<String> promoters = map.get("dc_contributor_advisor");
            if (promoters != null && !promoters.isEmpty()) {
                resultString.add("Promoters: " + promoters.stream().reduce("", parser));
            }
            List<String> abstractText = map.get("dc_description_abstract");
            if (abstractText != null && !abstractText.isEmpty()) {
                resultString.add("Abstract: " + abstractText.get(0));
            }
            if (resultString.isEmpty()) {
                resultString.add(":: No valid metadata could be found for the thesis, please contact support ::");
            }
            return String.join("\n", resultString);
        } catch (Exception e) {
            throw new ResumeGenerationException("Submission mail generation failed :: " + e.getMessage());
        }
    }

    /**
     * Generates a base email version with the given metadata that can be used for both the authors and the promoters.
     * The returned Email object can be then further modified before sending.
     *
     * @param metadata A HashMap containing all the metadata of the submission.
     * @param templatePath The path to the email template to use.
     * @param recipients The list of recipients which need to receive the email.
     * @param attachment The PDF attestation to attach to the email.
     * @return The generated email object.
     * @throws ResumeGenerationException
     * @throws IOException
     */
    private Email generateBaseEmail(HashMap<String, List<String>> metadata, String templatePath,
                                    List<String> recipients, InputStream attachment)
            throws ResumeGenerationException, IOException {
        Email email = Email.getEmail(templatePath);
        addRecipients(metadata, recipients, email);
        email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
        email.addArgument(metadata.get("dspace_entity_type").get(0).toLowerCase());
        email.addArgument(generateSubmissionResume(metadata));
        email.setSubject(mailSubject);
        appendAttachmentToEmail(attachment, metadata, email);
        return email;
    }

    /**
     * Take an Email object and append an InputStream to it. In this case, this is an attestation.
     *
     * @param attestation The attestation as an InputStream.
     * @param metadata A HashMap containing all the metadata of the submission.
     * @param email The email object to append the attachment to.
     */
    private void appendAttachmentToEmail(InputStream attestation, HashMap<String, List<String>> metadata, Email email) {
        email.addAttachment(
                attestation,
                metadata.get("dspace_entity_type").get(0) + "SubmissionAttestation.pdf", "application/pdf"
        );
    }

    /**
     * Used for supervisor emails, appends access URLs for the bitstreams to the email.
     *
     * @param metadata A HashMap containing all the metadata of the submission.
     * @param email The email object to append the URLs to.
     * @param item The DSpace item corresponding to the submission.
     */
    private void appendUrlsToEmail(HashMap<String, List<String>> metadata, Email email, Item item) {
        List<String> urls = new ArrayList<>();
        try {
            String supervisorEmail = metadata.get("advisors_email").get(0);
            if (supervisorEmail == null) {
                throw new Exception("Tried to generate access URLs for the promoters but no email was found.");
            }
            DirectLinkGenerator linkGenerator = directLinkGeneratorFactory.getGenerator("thesisSupervisor");
            List<Bitstream> bitstreams = item.getBundles(CONTENT_BUNDLE_NAME)
                    .stream()
                    .flatMap(bundle -> bundle.getBitstreams().stream())
                    .toList();
            Map<String, Object> args = Map.of("email", supervisorEmail);
            for (Bitstream bitstream : bitstreams) {
                urls.add(linkGenerator.buildURL(bitstream, args));
            }
        } catch (Exception e) {
            logger.error("Unable to generate direct download URLs for supervisors :: " + e.getMessage());
        } finally {
            email.addArgument(urls);
        }
    }

    /**
     * Used to add recipients to an Email object.
     * Two cases exist:
     *   * use the configuration (if it exists) found for the 'uclouvain.pdf_attestation.mail.recipients' key and use
     *     it as recipient;
     *   * use a provided list of metadata keys that will be used to retrieve recipients;
     *
     * @param itemMetadata A list of metadata key used to generate the recipients.
     * @param metadataToLookup A HashMap containing all the metadata of the submission.
     * @param email The email object to append the recipients to.
     */
    private void addRecipients(HashMap<String, List<String>> itemMetadata, List<String> metadataToLookup, Email email) {
        if (recipientsConfig != null && !recipientsConfig.isEmpty()) {
            for (String recipient: recipientsConfig) {
                email.addRecipient(recipient);
            }
        } else {
            for (String metadata: metadataToLookup) {
                for (String address: itemMetadata.get(metadata)) {
                    email.addRecipient(address);
                }
            }
        }
    }
}