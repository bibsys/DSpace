package org.dspace.uclouvain.core.mails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dspace.content.Item;
import org.dspace.core.Email;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.core.GenericThesisEmail;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.exceptions.mail.EmailGenerationException;

/**
 * Email to be sent when an error occurres generating {@link ThesisAuthorAttestationEmail} or {@link ThesisPromoterAttestationEmail}.
 * It is meant to be sent to both the manager and the promoters.
 * 
 * @Authored-by: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ThesisErrorAttestationEmail extends GenericThesisEmail {

    protected String mailErrorSubject = this.configService.getProperty("uclouvain.pdf_attestation.mail.error.subject");
    protected String entityTypeField = new MetadataField("dspace.entity.type").getFullString("_");
    protected String authorEmailField = new MetadataField(this.configService.getProperty("uclouvain.global.metadata.authoremail.field", "authors.email")).getFullString("_");
    protected String promoterEmailField = new MetadataField(this.configService.getProperty("uclouvain.global.metadata.advisoremail.field", "advisors.email")).getFullString("_");
    
    protected Exception error;

    public ThesisErrorAttestationEmail(Item item, Exception error) {
        super(item);
        this.error = error;
    }

    /**
     * Configuration used to find the different properties for the email (subject, recipients...)
     */
    protected String getConfigurationName() {
        return "pdf_attestation";
    }

    /**
     * Get the corresponding template file for the error attestation email.
     */
    protected String getTemplatePath(){
        return this.source + "/config/emails/pdf_attestation_error";
    }

    /**
     * Recover the list of recipients for the error email.
     * In this case we notify both authors && promoters.
     *
     * @return: The list of email adresses to be used as recipients.
     */
    protected List<String> getRecipientsEmails() {
        List<String> recipients = this.metadataMap.get(authorEmailField);
        recipients.addAll(this.metadataMap.get(promoterEmailField));
        return recipients;
    }

    /**
     * Send an error email to both the submitters and the promoters with the stacktrace of the exception.
     * @param email: The current email to modify.
     * @throws EmailGenerationException: An error occurred while filling the email with information.
     */
    protected void generateEmail(Email email) throws EmailGenerationException {
        try {
            this.addRecipients(this.getRecipientsEmails(), email);

            EPerson submitter = item.getSubmitter();

            email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
            email.addArgument(submitter.getFullName());
            email.addArgument(this.metadataMap.get(this.entityTypeField).get(0).toLowerCase());
            email.addArgument("Here are some information that might be useful for our team:\n -> Item's uuid: " + this.item.getID() + "\n-> Stacktrace:\n" + ExceptionUtils.getStackTrace(this.error));
            
            email.setSubject(this.mailErrorSubject);
        } catch (Exception e) {
            throw new EmailGenerationException("COuld not generate attestation error email", e);
        }
    }
}
