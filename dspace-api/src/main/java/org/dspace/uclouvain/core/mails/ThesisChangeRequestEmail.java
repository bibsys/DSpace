/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Email;
import org.dspace.uclouvain.core.GenericThesisEmail;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.exceptions.EmailGenerationException;

/**
 * Class representing the ChangeRequest email.
 * This email is to be sent when a manager requests a change for a workflow item.
 * It will send the email along with the data to both the submitter and the promoters.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ThesisChangeRequestEmail extends GenericThesisEmail {

    protected String changeRequest;
    protected String promoterEmailField = new MetadataField(configService
            .getProperty("uclouvain.global.metadata.advisoremail.field", "advisors.email"))
            .getFullString("_");

    public ThesisChangeRequestEmail(Item item, String reason) {
        super(item);
        changeRequest = reason;
    }

    /**
     * Get the author email addresses that will be used as recipients.
     * @return The recipient addresses list.
     */
    protected List<String> getRecipientsEmails() {
        List<String> recipients = metadataMap.get(promoterEmailField);
        recipients.add(item.getSubmitter().getEmail());
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
     * Fill the email with information: recipients, subjects and arguments for the template.
     * @param email the email to fill.
     * @throws EmailGenerationException if an error occurs while filling email information.
     */
    @Override
    protected void generateEmail(Email email) throws EmailGenerationException {
        try {
            this.addRecipients(this.getRecipientsEmails(), email);
            email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
            email.addArgument(this.metadataMap.get("dc_title").get(0));
            email.addArgument(this.changeRequest);
            email.setSubject(this.mailSubject);
        } catch (Exception e) {
            throw new EmailGenerationException("An error occurred while filling email informations.", e);
        }
    }
}