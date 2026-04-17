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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import jakarta.mail.MessagingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.mails.metadataParser.MailMetadataParserService;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;
import org.dspace.uclouvain.exceptions.EmailGenerationException;
import org.dspace.uclouvain.exceptions.SendEmailException;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;

/**
 * Super abstract class that provide a base for all UCLouvainEmail classes.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (michotte.renaud@uclouvain.be)
 */
public abstract class AbstractUCLouvainEmail implements UCLouvainEmail {

    // ATTRIBUTES ======================================================================================================
    protected String mailSubject;
    protected List<String> forcedRecipients;
    protected String replyTo;
    protected HashMap<String, List<String>> metadataMap;
    protected Item item;
    protected Context context;
    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected MailMetadataParserService mailMetadataParserService = UCLouvainServiceFactory
        .getInstance().getMailMetadataParserService();
    protected String source = configService.getProperty("dspace.dir");
    protected static final Logger log = LogManager.getLogger(AbstractUCLouvainEmail.class);

    // ABSTRACT METHODS ================================================================================================
    protected abstract String getTemplatePath();
    protected abstract String buildMailSubject();
    protected abstract void generateEmail(Email email, Item item) throws EmailGenerationException;
    protected abstract String getConfigurationName();
    protected abstract List<String> getRecipientAddresses();
    protected abstract boolean isValidForItem(Context context, Item item);

    // METHODS ==================================================================================================
    public AbstractUCLouvainEmail(Context context, Item item) throws EmailFailedInitException {
        if (item == null) {
            throw new EmailFailedInitException("Given item is null for email class " + this.getClass());
        }
        this.context = context;
        this.item = item;
        this.metadataMap = MetadataUtils.getValuesHashMap(item.getMetadata());
        this.mailSubject = getConfigurationAttribute("subject");
        this.forcedRecipients = Arrays.asList(getConfigurationAttributes("recipients"));
        this.replyTo = getReplyTo();
    }

    protected String getConfigurationAttribute(String attribute) {
        return Arrays.stream(getConfigurationAttributes(attribute)).findFirst().orElse(null);
    }

    protected String[] getConfigurationAttributes(String attribute) {
        String propertyKey = String.format("uclouvain.%s.mail.%s", getConfigurationName(), attribute);
        return configService.getArrayProperty(propertyKey, new String[0]);
    }

    /**
     * Get the 'reply-to' address from configuration.
     * First check if it exists at the email level. If not use the global default config.
     * 
     * @return The 'reply-to' address to use.
     */
    protected String getReplyTo() {
        String configuredReplyTo = getConfigurationAttribute("reply-to");
        return (configuredReplyTo != null)
            ? configuredReplyTo
            : configService.getProperty("uclouvain.default.mail.reply-to");
    }

    /**
     * Get the CC addresses to use for this email.
     * By default, none address will be used as CC address.
     *
     * @return the list of email addresses to use.
     */
    protected List<String> getCCAddresses() {
        return Collections.emptyList();
    }

    /**
     * Creates and sends an email base on the given configuration (through constructor).
     * @throws EmailGenerationException if the addition of the email arguments failed.
     * @throws SendEmailException if errors occurred during email sent
     */
    public void sendEmail() throws EmailGenerationException, SendEmailException {
        if (!isValidForItem(context, item)) {
            throw new EmailGenerationException(
                "Item [" + item.getID() + "] is not processable by email class " + this.getClass()
            );
        }
        try {
            Email email = Email.getEmail(getTemplatePath());
            email.setSubject(buildMailSubject());
            email.setReplyTo(replyTo);
            email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
            filterRecipients(getRecipientAddresses()).forEach(email::addRecipient);
            filterRecipients(getCCAddresses()).forEach(email::addCcAddress);
            generateEmail(email, item); // build email content and build additional arguments
            email.send();
        } catch (IOException | MessagingException e) {
            throw new SendEmailException("Failed to call .send() on the generated email.", e);
        }
    }

    // PRIVATE METHODS =================================================================================================
    /**
     * Filter recipient addresses bases on configuration.
     * Either parameter exists into configuration --> in this case, we force to use them (override recipients argument)
     * Either no special configuration is specified --> filter recipients argument restriction by some domain
     *
     * @param recipients the list recipient email addresses
     * @return a stream of email addresses that can be used.
     */
    private Stream<String> filterRecipients(List<String> recipients) {
        return (!forcedRecipients.isEmpty())
            ? forcedRecipients.stream()
            : recipients.stream().filter(this::isValidSuffix);
    }

    /**
     * Checks the validity of an address.
     * An address is valid if it contains a configured suffix.
     *
     * @param address the address to validate.
     * @return True if the address contains at least one of the configured suffixes, 'false' if not.
     */
    private Boolean isValidSuffix(String address) {
        List<String> validEmailSuffix = List.of(this.getConfigurationAttributes("suffixes"));
        return validEmailSuffix.isEmpty() || validEmailSuffix.stream().anyMatch(address::endsWith);
    }
}
