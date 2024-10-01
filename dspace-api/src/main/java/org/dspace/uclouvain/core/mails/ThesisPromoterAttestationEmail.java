/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Email;
import org.dspace.uclouvain.core.Hasher;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.exceptions.EmailGenerationException;

/**
 * Main class to send an email for the submission attestation to the promoters of the item.
 * This mail is sent when someone makes a new submission, and it enters the workflow validation system.
 * This class extends {@link ThesisAuthorAttestationEmail} and adds the generation of file access links for promoters.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ThesisPromoterAttestationEmail extends ThesisAuthorAttestationEmail {


    private Logger logger = LogManager.getLogger(ThesisPromoterAttestationEmail.class);

    protected String backendURL = configService.getProperty("dspace.server.url");
    protected String promoterEmailField = new MetadataField(configService
            .getProperty("uclouvain.global.metadata.advisoremail.field", "advisors.email"))
            .getFullString("_");
    private String algorithm;
    private String encryptionKey;


    public ThesisPromoterAttestationEmail(Item item, InputStream attachment, String algorithm, String encryptionKey) {
        super(item, attachment);
        this.algorithm = algorithm;
        this.encryptionKey = encryptionKey;
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
        return this.source + "/config/emails/pdf_attestation_promoter";
    }

    /**
     * Get the promoter email addresses that will be used as recipients.
     * @return the recipient addresses list.
     */
    @Override
    protected List<String> getRecipientsEmails() {
        return this.metadataMap.get(this.promoterEmailField);
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
            if (this.encryptionKey.isEmpty()) {
                logger.error("!! NO ENCRYPTION KEY PROVIDED FOR BITSTREAM PROMOTER HASHING !!");
                return;
            }
            Hasher hasher = new Hasher(this.algorithm, this.encryptionKey);
            String promoter = metadata.get(this.promoterEmailField).get(0);
            if (promoter != null) {
                String promoterHash = hasher.processHashAsString(promoter);
                Bundle bitstreamBundle = dspaceItem.getBundles("ORIGINAL").get(0);
                for (Bitstream bitstream: bitstreamBundle.getBitstreams()) {
                    urls.add(
                        bitstream.getName() + ": " + this.backendURL + "/api/uclouvain/bitstream/"
                        + bitstream.getID() + "/content?hash=" + promoterHash);
                }
                email.addArgument(urls);
            } else {
                logger.warn("Tried to generate access URLs for the promoters but no email was found.");
            }
        } catch (NoSuchAlgorithmException e) {
            logger.warn("'" + this.algorithm + "' is not a known algorithm name.", e);
        } catch (Exception e) {
            logger.error("Could not generate URLs for the promoters (unhandled exception): " + e.getMessage());
        }
    }
}
