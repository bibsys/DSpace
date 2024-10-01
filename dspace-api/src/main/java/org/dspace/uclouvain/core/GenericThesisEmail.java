/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.mail.MessagingException;
import org.dspace.content.Item;
import org.dspace.core.Email;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.exceptions.EmailGenerationException;

/**
 * Generic class to create a thesis email.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public abstract class GenericThesisEmail {
    protected String mailSubject;
    protected List<String> recipientsConfig;
    protected HashMap<String, List<String>> metadataMap;
    protected Item item;

    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected String source = configService.getProperty("dspace.dir");

    protected abstract String getTemplatePath();
    protected abstract void generateEmail(Email email) throws EmailGenerationException;
    protected abstract String getConfigurationName();

    protected GenericThesisEmail(Item item) {
        this.item = item;
        this.metadataMap = MetadataUtils.getValuesHashMap(item.getMetadata());
        this.mailSubject = getConfigurationAttribute("subject");
        this.recipientsConfig = Arrays.asList(getConfigurationAttributes("recipients"));
    }

    protected String getConfigurationAttribute(String attribute) {
        String[] properties = getConfigurationAttributes(attribute);
        return (properties.length > 0)
            ? properties[0]
            : null;
    }

    protected String[] getConfigurationAttributes(String attribute) {
        String propertyKey = "uclouvain." + getConfigurationName() + ".mail." + attribute;
        return configService.getArrayProperty(propertyKey, new String[0]);
    }

    /**
     * Creates and sends an email base on the given configuration (through constructor).
     * @throws IOException if the creation of the base email using the template was not a success.
     * @throws EmailGenerationException if the addition of the email arguments failed.
     * @throws MessagingException if errors occurred during email sent
     */
    public void sendEmail() throws IOException, EmailGenerationException, MessagingException {
        Email email = Email.getEmail(getTemplatePath());
        generateEmail(email);
        email.send();
    }

    /**
     * Used to add recipients to an Email object.
     * Two cases exist:
     *   -> use the configuration (if it exists) found for the 'uclouvain.pdf_attestation.mail.recipients'
     *      key and use it as recipient;
     *   -> use a provided list of a metadata key that will be used to retrieve recipients;
     * @param recipients the list recipient email addresses
     * @param email The email object
     */
    protected void addRecipients(List<String> recipients, Email email) {
        List<String> filteredRecipients = (recipientsConfig != null && !recipientsConfig.isEmpty())
                ? recipientsConfig
                : recipients.stream().filter(this::isValidAddress).collect(Collectors.toList());
        for (String recipient: filteredRecipients) {
            email.addRecipient(recipient);
        }
    }

    /**
     * Checks the validity of an address.
     * An address is valid if it contains a configured suffix.
     * @param address the address to validate.
     * @return 'true' if the address contains at least one of the configured suffixes, 'false' if not.
     */
    protected Boolean isValidAddress(String address) {
        for (String suffix: getConfigurationAttributes("suffixes")) {
            if (address.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
