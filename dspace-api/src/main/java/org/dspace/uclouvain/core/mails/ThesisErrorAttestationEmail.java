/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;
import org.dspace.uclouvain.exceptions.EmailGenerationException;

/**
 * Email to send when an error occurred during generation of attestation email (author or supervisor).
 * It is meant to be sent to thesis authors.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class ThesisErrorAttestationEmail extends GenericThesisEmail {
    protected Exception error;
    public ThesisErrorAttestationEmail(Context context, Item item, Exception error) throws EmailFailedInitException {
        super(context, item);
        this.error = error;
    }

    @Override
    protected String getConfigurationName() {
        return "pdf_attestation";
    }

    @Override
    protected List<String> getRecipientAddresses() {
        return itemService.getMetadataByMetadataString(item, authorEmailField)
                .stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
    }

    @Override
    protected List<String> getCCAddresses() {
        return Collections.singletonList(configService.getProperty("mail.admin"));
    }

    @Override
    protected String getTemplatePath() {
        return this.source + "/config/emails/thesis_attestation.error";
    }

    @Override
    protected String buildMailSubject() {
        return configService.getProperty("uclouvain.pdf_attestation.mail.error.subject");
    }

    /**
     * Send an error email to both the submitters and the promoters with the stacktrace of the exception.
     * @param email the current email to modify.
     * @param item The item used to generate the email.
     * @throws EmailGenerationException an error occurred while filling the email with information.
     */
    protected void generateEmail(Email email, Item item) throws EmailGenerationException {
        try {
            EPerson submitter = item.getSubmitter();
            email.addArgument(submitter.getFullName());
            email.addArgument(configService.getProperty("dspace.ui.url") + "/mydspace");
            email.addArgument(item.getID());
            email.addArgument(ExceptionUtils.getStackTrace(error));
        } catch (Exception e) {
            throw new EmailGenerationException("Could not generate attestation error email", e);
        }
    }
}
