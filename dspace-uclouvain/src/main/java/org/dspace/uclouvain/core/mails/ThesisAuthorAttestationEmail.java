package org.dspace.uclouvain.core.mails;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BinaryOperator;

import org.dspace.content.Item;
import org.dspace.core.Email;
import org.dspace.uclouvain.core.GenericThesisEmail;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.exceptions.mail.EmailGenerationException;
import org.dspace.uclouvain.exceptions.mail.ResumeGenerationException;

/**
 * Main class to send an email for the submission attestation to the authors of the item.
 * This mail is sent when someone makes a new submission and it enters the workflow validation system.
 * 
 * @Authored-by: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ThesisAuthorAttestationEmail extends GenericThesisEmail {

    protected String entityTypeField = new MetadataField("dspace.entity.type").getFullString("_");
    protected String authorNameField = new MetadataField(this.configService.getProperty("uclouvain.global.metadata.authorname.field", "dc.contributor.author")).getFullString("_"); 
    protected String authorEmailField = new MetadataField(this.configService.getProperty("uclouvain.global.metadata.authoremail.field", "authors.email")).getFullString("_");
    protected String promoterNameField = new MetadataField(this.configService.getProperty("uclouvain.global.metadata.advisorname.field", "dc.contributor.advisor")).getFullString("_");

    protected InputStream attachment;

    public ThesisAuthorAttestationEmail(Item item, InputStream attachment) {
        super(item);
        this.attachment = attachment;
    }

    /**
     * Configuration used to find the different properties for the email (subject, recipients...)
     */
    protected String getConfigurationName() {
        return "pdf_attestation";
    }

    /**
     * Get the corresponding template file for the author attestation mail.
     */
    protected String getTemplatePath() {
        return this.source + "/config/emails/pdf_attestation_author";
    }

    /**
     * Generates a base email version with the given metadata that can be used for both the authors and the promoters.
     * @param email: The current email to modify.
     * @throws EmailGenerationException: If an error occurs while filling email information.
     */
    protected void generateEmail(Email email) throws EmailGenerationException {
        try {
            this.addRecipients(this.getRecipientsEmails(), email);
            email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
            email.addArgument(this.metadataMap.get(entityTypeField).get(0).toLowerCase());
            email.addArgument(generateSubmissionResume(this.metadataMap));
            email.setSubject(this.mailSubject);
            this.appendAttachmentToEmail(this.attachment, this.metadataMap, email);
        } catch(Exception e) {
            throw new EmailGenerationException("An error occurred while filling email informations.", e);
        }
    }

    /**
     * Get the author email adresses that will be used as recipients.
     * @return The recipients adresses.
     */
    protected List<String> getRecipientsEmails() {
        return this.metadataMap.get(this.authorEmailField);
    }

    /**
     * Generates a resume for the given submission containing the title, the abstract and the authors.
     * 
     * @param metadataMap The HashMap containing information about the submission.
     * @return The resume as a String.
    */
    protected String generateSubmissionResume(HashMap<String, List<String>> metadataMap) throws ResumeGenerationException {
        try {
            BinaryOperator<String> parser = (subtotal, element) -> subtotal + element + "; ";
            List<String> resultString = new ArrayList<String>();

            // Retrieve all required metadata && check if they are existing before adding them to the submission's resume.
            List<String> title = metadataMap.get("dc_title");
            if (title != null && !title.isEmpty()) {
                resultString.add("Title: " + title.get(0));
            }

            List<String> authors = metadataMap.get(this.authorNameField);
            if (authors != null && !authors.isEmpty()){
                resultString.add("Authors: " + authors.stream().reduce("", parser));
            }

            List<String> promoters = metadataMap.get(this.promoterNameField);
            if (promoters != null && !promoters.isEmpty()){
                resultString.add("Promoters: " + promoters.stream().reduce("", parser));
            }

            List<String> abstractText = metadataMap.get("dc_description_abstract");
            if (abstractText != null && !abstractText.isEmpty()){
                resultString.add("Abstract: " + abstractText.get(0));
            }

            if (resultString.size() == 0) {
                resultString.add(":: No valid metadata could be found for the thesis, please contact support ::");
            }
            
            return String.join("\n", resultString);
        } catch (Exception e) {
            throw new ResumeGenerationException("Submission mail generation failed :: " + e.getMessage());
        }
    }

    /**
     * Take an Email object and append an InputStream to it. In this case an attestation. 
     * @param attestation: The attestation as an InputStream.
     * @param metadata: A HashMap containing all the metadata of the submission.
     * @param email: The email object to append the attachment to.
     */
    private void appendAttachmentToEmail(InputStream attestation, HashMap<String, List<String>> metadata, Email email) {
        email.addAttachment(attestation, metadata.get(entityTypeField).get(0) + "SubmissionAttestation.pdf", "application/pdf");
    }
}
