package org.dspace.uclouvain.core;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.mail.MessagingException;

import org.dspace.content.Item;
import org.dspace.core.Email;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.exceptions.mail.EmailGenerationException;

/**
 * Generic class to extend in order to create a thesis email sending system.
 */
public abstract class GenericThesisEmail {
    protected String mailSubject;
    protected List<String> recipientsConfig;
    protected HashMap<String, List<String>> metadataMap;
    protected Item item;

    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected String source = this.configService.getProperty("dspace.dir");

    protected abstract String getTemplatePath();
    protected abstract void generateEmail(Email email) throws EmailGenerationException;

    protected abstract String getConfigurationName();

    protected GenericThesisEmail(Item item) {
        this.item = item;
        this.metadataMap = MetadataUtils.getValuesHashMap(item.getMetadata());
        this.mailSubject = this.getConfigurationAttribute("subject");
        this.recipientsConfig = Arrays.asList(this.getConfigurationAttributes("recipients"));
    }

    protected String getConfigurationAttribute(String attribute) {
        return this.configService.getProperty("uclouvain." + getConfigurationName() + ".mail." + attribute, null);
    }

    protected String[] getConfigurationAttributes(String attribute) {
        return this.configService.getArrayProperty("uclouvain." + getConfigurationName() + ".mail." + attribute, new String[0]);
    }

    /**
     * Creates and sends an email base on the given configuration (through constructor).
     * @throws IOException: If the creation of the base email using the template was not a success.
     * @throws EmailGenerationException: If the addition of the email arguments failed.
     * @throws MessagingException: Failed to send the email to recipients.
     */
    public void sendEmail() throws IOException, EmailGenerationException, MessagingException {
        Email email = Email.getEmail(this.getTemplatePath());
        this.generateEmail(email);
        email.send();
    }

    /**
     * Used to add recipients to an Email object.
     * 2 cases:
     * -> use the configuration (if it exist) found for the 'uclouvain.pdf_attestation.mail.recipients' key and use it as recipient;
     * -> use a provided list of metadata key that will be used to retrieve recipients;
     * @param itemMetadata: A list of metadata key used to generate the recipients.
     * @param metadataToLookup: A HashMap containing all the metadata of the submission.
     * @param email: The email object to append the recipients to.
     */
    protected void addRecipients(List<String> destinationEmails, Email email){
        if (this.recipientsConfig != null && !this.recipientsConfig.isEmpty()) {
            for (String recipient: this.recipientsConfig) {
                email.addRecipient(recipient);
            }
        } else {
            for (String destinationEmail: destinationEmails) {
                if (this.isValidAddress(destinationEmail)) email.addRecipient(destinationEmail);
            }
        }
    }

    /**
     * Checks the validity of an address.
     * An address is valid if it contains a configured suffix.
     * @param address: The address to validate.
     * @return: Returns 'true' if the address contains at least one of the configured suffix, 'false' if not.
     */
    protected Boolean isValidAddress(String address){
        for (String suffix: this.getConfigurationAttributes("suffixes")) {
            if (address.endsWith(suffix)) return true;
        }
        return false;
    }
}
