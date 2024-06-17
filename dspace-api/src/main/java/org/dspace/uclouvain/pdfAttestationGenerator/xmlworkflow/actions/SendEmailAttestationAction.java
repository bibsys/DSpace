/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pdfAttestationGenerator.xmlworkflow.actions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.BinaryOperator;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.uclouvain.core.Hasher;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.HandlerNotFoundException;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.ResumeGenerationException;
import org.dspace.uclouvain.pdfAttestationGenerator.factory.PDFAttestationGeneratorFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.eperson.EPerson;
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

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private final String backendURL = configService.getProperty("dspace.server.url");
    private final String source = configService.getProperty("dspace.dir");
    private final String mailSubject = configService.getProperty("uclouvain.pdf_attestation.mail.subject");
    private final String mailErrorSubject = configService.getProperty("uclouvain.pdf_attestation.mail.error.subject");

    private final List<String> recipientsConfig = Arrays.asList(
        configService.getArrayProperty("uclouvain.pdf_attestation.mail.recipients", new String[0]));
    private final String[] validAddressSuffix =
            configService.getArrayProperty("uclouvain.pdf_attestation.mail.suffixes", new String[0]);

    private final String algorithm = configService.getProperty("uclouvain.api.bitstream.download.algorithm", "MD5");
    private final String encryptionKey = configService.getProperty("uclouvain.api.bitstream.download.secret", "");

    private Logger logger = LogManager.getLogger(SendEmailAttestationAction.class);

    // FIELD CONFIGURATION
    private String authorEmailField;
    private String authorNameField;
    private String promoterEmailField;
    private String promoterNameField;
    private String entityTypeField;

    public SendEmailAttestationAction() throws Exception {
        // Instantiate the metadata fields from the configuration
        authorEmailField = new MetadataField(
            configService.getProperty("uclouvain.global.metadata.authoremail.field", "authors.email")
        ).getFullString("_");
        authorNameField = new MetadataField(
            configService.getProperty("uclouvain.global.metadata.authorname.field", "dc.contributor.author")
        ).getFullString("_");
        promoterEmailField = new MetadataField(
            configService.getProperty("uclouvain.global.metadata.advisoremail.field", "advisors.email")
        ).getFullString("_");
        promoterNameField = new MetadataField(
            configService.getProperty("uclouvain.global.metadata.advisorname.field", "dc.contributor.advisor")
        ).getFullString("_");
        entityTypeField = new MetadataField(
            configService.getProperty("uclouvain.global.metadata.entitytype.field", "dspace.entity.type")
        ).getFullString("_");
    }

    @Override
    public void activate(Context context, XmlWorkflowItem wf){}

    /**
    * Create an email with information from the submission, attach the PDF attestation and send it to the submitter.
    * Action used by the workflow system to send an email when a submission is made.
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

                // Checks if authors and promoter are present
                if (map.get(this.authorEmailField) == null || map.get(promoterEmailField) == null) {
                    logger.warn("No authors or advisors found for the following item: " + dspaceItem.getID()
                            + ". Aborting email attestation generation.");
                    return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
                }

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
        } catch (HandlerNotFoundException e) {
            logger.error("No handler found for item with uuid: " + uuid + ": " + e.getMessage());
            return new ActionResult(ActionResult.TYPE.TYPE_ERROR);
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
        Email email = generateBaseEmail(metadata, templatePath, Arrays.asList(this.authorEmailField), attestation);
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
        Email email = generateBaseEmail(metadata, templatePath, Arrays.asList(this.promoterEmailField), attestation);
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
        addRecipients(metadata, Arrays.asList(this.authorEmailField, this.promoterEmailField), email);
        email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
        email.addArgument(submitter.getFullName());
        email.addArgument(metadata.get(this.entityTypeField).get(0).toLowerCase());
        email.addArgument("Here are some information that might be useful for our team:\n -> Item's uuid: "
                + item.getID() + "\n-> Stacktrace:\n" + ExceptionUtils.getStackTrace(exception));
        email.setSubject(mailErrorSubject);
        email.send();
    }

    /**
     * Generates a resume for the given submission containing the title, the abstract and the authors.
     * 
     * @param map The HashMap containing information about the submission.
     * @return The resume as a String.
    */
    private String generateSubmissionResume(HashMap<String, List<String>> map) throws ResumeGenerationException {
        try {
            BinaryOperator<String> parser = (subtotal, element) -> subtotal + element + "; ";
            List<String> resultString = new ArrayList<>();
            // Retrieve all required metadata && check if they are existing before adding them to the submission's
            // resume.
            List<String> title = map.get("dc_title");
            if (title != null && !title.isEmpty()) {
                resultString.add("Title: " + title.get(0));
            }
            List<String> authors = map.get(this.authorNameField);
            if (authors != null && !authors.isEmpty()) {
                resultString.add("Authors: " + authors.stream().reduce("", parser));
            }
            List<String> promoters = map.get(this.promoterNameField);
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
        email.addArgument(metadata.get(entityTypeField).get(0).toLowerCase());
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
                metadata.get(entityTypeField).get(0) + "SubmissionAttestation.pdf", "application/pdf"
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
            if (this.encryptionKey.isEmpty()) {
                logger.error("!! NO ENCRYPTION KEY PROVIDED FOR BITSTREAM PROMOTER HASHING !!");
                return;
            }
            Hasher hasher = new Hasher(algorithm, encryptionKey);
            String promoter = metadata.get(this.promoterEmailField).get(0);
            if (promoter != null) {
                String promoterHash = hasher.processHashAsString(promoter);
                Bundle bitstreamBundle = item.getBundles("ORIGINAL").get(0);
                for (Bitstream bitstream: bitstreamBundle.getBitstreams()) {
                    urls.add(bitstream.getName() + ": " + backendURL + "/api/uclouvain/bitstream/" + bitstream.getID()
                            + "/content?hash=" + promoterHash);
                }
                email.addArgument(urls);
            } else {
                logger.warn("Tried to generate access URLs for the promoters but no email was found.");
            }
        } catch (NoSuchAlgorithmException e) {
            logger.warn("'" + algorithm + "' is not a known algorithm name.", e);
        } catch (Exception e) {
            logger.error("Could not generate URLs for the promoters (unhandled exception): " + e.getMessage());
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
                    if (this.isValidAddress(address)) {
                        email.addRecipient(address);
                    }
                }
            }
        }
    }

    /**
     * Checks the validity of an address.
     * An address is valid if it contains a configured suffix.
     *
     * @param address The address to validate.
     * @return Returns 'true' if the address contains at least one of the configured suffixes, 'false' if not.
     */
    private Boolean isValidAddress(String address) {
        for (String suffix: this.validAddressSuffix) {
            if (address.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}