/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.uclouvain.exceptions.EmailGenerationException;

/**
 * Class representing the ChangeRequest email.
 * This email is to be sent when a manager requests a change for a workflow item.
 * It will send the email along with the data to both the submitter and authors.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ThesisChangeRequestEmail extends GenericThesisEmail {

    protected String changeRequest;
    protected String authorEmailField = configService
            .getProperty("uclouvain.global.metadata.authoremail.field", "advisors.email");

    public ThesisChangeRequestEmail(Context context, Item item, String reason) {
        super(context, item);
        changeRequest = reason;
    }

    @Override
    protected List<String> getRecipientAddresses() {
        List<String> recipients = itemService.getMetadataByMetadataString(item, authorEmailField)
                .stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
        String submitterEmail = item.getSubmitter().getEmail();
        if (!recipients.contains(submitterEmail)) {
            recipients.add(submitterEmail);
        }
        return recipients;
    }

    @Override
    protected String getConfigurationName() {
        return "change_request";
    }

    @Override
    protected String getTemplatePath() {
        return this.source + "/config/emails/thesis_change_request";
    }

    @Override
    protected String buildMailSubject() {
        return this.mailSubject;
    }

    @Override
    protected void generateEmail(Email email) throws EmailGenerationException {
        try {
            email.addArgument(itemService.getMetadata(item, "dc.title"));
            email.addArgument(this.changeRequest);
            email.addArgument(configService.getProperty("dspace.ui.url") + "/mydspace");
        } catch (Exception e) {
            throw new EmailGenerationException("An error occurred while filling email informations.", e);
        }
    }
}