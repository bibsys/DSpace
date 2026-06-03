/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import jakarta.mail.MessagingException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;
import org.dspace.uclouvain.exceptions.EmailGenerationException;
import org.dspace.uclouvain.exceptions.SendEmailException;

/**
 * Email to notify of an error when enhancing an item.
 * This mail is meant to be send to DIAL staff, it must indicate the uuid of the item and the error stacktrace.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class EnhancementErrorNotifyEmail {
    protected static final String TEMPLATE_PATH = "/config/emails/enhancement_error_notify";
    protected static final String MAIL_SUBJECT = "[DIAL] An error occurred while enhancing an item.";
    protected static final String MAIL_REPLY_TO = "noreply@uclouvain.be";
    protected static final String MAIN_RECIPIENT = "bibsys@uclouvain.be";

    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    protected UUID uuid;
    protected String entityType;
    protected Exception error;
    protected String source = configService.getProperty("dspace.dir");
    protected String instanceName = configService.getProperty("dspace.name");

    public EnhancementErrorNotifyEmail(
        Context context, UUID uuid, String entityType, Exception error
    ) throws EmailFailedInitException {
        this.uuid = uuid;
        this.entityType = entityType;
        this.error = error;
    }

    public void sendEmail() throws EmailGenerationException, SendEmailException {
        try {
            Email email = Email.getEmail(this.source + TEMPLATE_PATH);

            email.setSubject(MAIL_SUBJECT);
            email.setReplyTo(MAIL_REPLY_TO);
            email.addRecipient(MAIN_RECIPIENT);
            email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
            email.addArgument(uuid);
            email.addArgument(entityType);
            email.addArgument(instanceName);
            email.addArgument(ExceptionUtils.getStackTrace(error));
            email.send();
        } catch (IOException | MessagingException e) {
            throw new SendEmailException("Failed to call .send() on the generated email.", e);
        }
    }
}
