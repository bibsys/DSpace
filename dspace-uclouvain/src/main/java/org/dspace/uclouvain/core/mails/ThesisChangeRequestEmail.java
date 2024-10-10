package org.dspace.uclouvain.core.mails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Email;
import org.dspace.uclouvain.core.GenericThesisEmail;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.exceptions.mail.EmailGenerationException;

/**
 * Class representing the ChangeRequest email. This is email is to be sent when a manager requests a change for a workflow item.
 * It will send the email along with the data to both the submitter and the promoters.
 * 
 * @Authored-by: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ThesisChangeRequestEmail extends GenericThesisEmail {
    protected String changeRequest;

    protected String promoterEmailField = new MetadataField(this.configService.getProperty("uclouvain.global.metadata.advisoremail.field", "advisors.email")).getFullString("_");

    public ThesisChangeRequestEmail(Item item, String reason) {
        super(item);
        this.changeRequest = reason;
    }

    /**
     * Get the author email adresses that will be used as recipients.
     * @return The recipients adresses.
     */
    protected List<String> getRecipientsEmails() {
        List<String> recipients =  this.metadataMap.get(this.promoterEmailField);
        recipients.add(this.item.getSubmitter().getEmail());
        return recipients;
    }

    @Override
    protected String getConfigurationName() {
        return "change_request";
    }

    @Override
    protected String getTemplatePath() {
        return this.source + "/config/emails/change_request_notify_author";
    }

    /**
     * Fill th email with information: recipients, subjects and arguments for the template.
     * @param Email: The email to fill.
     * @throws EmailGenerationException: If an error occurs while filling email information.
     */
    @Override
    protected void generateEmail(Email email) throws EmailGenerationException {
        try {
            this.addRecipients(this.getRecipientsEmails(), email);
            email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
            email.addArgument(this.metadataMap.get("dc_title").get(0));
            email.addArgument(this.changeRequest);
            email.setSubject(this.mailSubject);
        } catch(Exception e) {
            throw new EmailGenerationException("An error occurred while filling email informations.", e);
        }
    }
}