/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import static org.dspace.core.Constants.CONTENT_BUNDLE_NAME;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Email;
import org.dspace.uclouvain.core.directLink.DirectLinkGenerator;
import org.dspace.uclouvain.core.directLink.DirectLinkGeneratorFactory;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.exceptions.EmailGenerationException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Main class to send an email for the submission attestation to the promoters of the item.
 * This mail is sent when someone makes a new submission, and it enters the workflow validation system.
 * This class extends {@link ThesisAuthorAttestationEmail} and adds the generation of file access links for promoters.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ThesisSupervisorAttestationEmail extends ThesisAuthorAttestationEmail {

    private final Logger logger = LogManager.getLogger(ThesisSupervisorAttestationEmail.class);

    @Autowired
    private DirectLinkGeneratorFactory directLinkGeneratorFactory;

    protected String supervisorEmailField = new MetadataField(
            configService.getProperty("uclouvain.global.metadata.advisoremail.field", "advisors.email")
        ).getFullString("_");
    protected String backendURL = configService.getProperty("dspace.server.url");

    public ThesisSupervisorAttestationEmail(Item item, InputStream attachment) {
        super(item, attachment);
    }

    @Override
    protected void generateEmail(Email email) throws EmailGenerationException {
        super.generateEmail(email);
        appendUrlsToEmail(metadataMap, email, item);
    }

    /**
     * Get the corresponding template file for the promoter attestation mail.
     */
    @Override
    protected String getTemplatePath() {
        return source + "/config/emails/pdf_attestation_promoter";
    }

    /**
     * Get the promoter email addresses that will be used as recipients.
     * @return the recipient addresses list.
     */
    @Override
    protected List<String> getRecipientsEmails() {
        return metadataMap.get(supervisorEmailField);
    }

    /**
     * Used for supervisor emails, appends access URLs for the bitstreams to the email.
     * @param metadata   a HashMap containing all the metadata of the submission.
     * @param email      the email object to append the URLs to.
     * @param dspaceItem the DSpace item corresponding to the submission.
     */
    protected void appendUrlsToEmail(HashMap<String, List<String>> metadata, Email email, Item dspaceItem) {
        List<String> urls = new ArrayList<>();
        try {
            String supervisorEmail = metadata.get(supervisorEmailField).get(0);
            if (supervisorEmail == null) {
                throw new Exception("Tried to generate access URLs for the promoters but no email was found.");
            }
            DirectLinkGenerator linkGenerator = directLinkGeneratorFactory.getGenerator("thesisSupervisor");
            List<Bitstream> bitstreams = item.getBundles(CONTENT_BUNDLE_NAME)
                    .stream()
                    .flatMap(bundle -> bundle.getBitstreams().stream())
                    .toList();
            for (Bitstream bitstream : bitstreams) {
                urls.add(linkGenerator.buildURL(bitstream, Map.of("email", supervisorEmail)));
            }
        } catch (Exception e) {
            logger.error("Unable to generate direct download URLs for supervisors :: " + e.getMessage());
        } finally {
            email.addArgument(urls);
        }
    }
}
