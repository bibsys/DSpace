/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import static org.dspace.uclouvain.constants.AccessConditions.EMBARGO;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.access.status.DefaultAccessStatusHelper;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.core.Hasher;
import org.dspace.uclouvain.exceptions.EmailGenerationException;
import org.dspace.uclouvain.exceptions.ResumeGenerationException;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.FacultyManagerService;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

/**
 * Main class to send an email for the submission attestation to the authors of the item.
 * This mail is sent when someone makes a new submission, and it enters the workflow validation system.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */

public class ThesisAuthorAttestationEmail extends GenericThesisEmail {

    // CONSTANTS =======================================================================================================
    private static final Map<String, Map<String, String>> FIELD_LABELS = Map.of(
            "authors", Map.of("fr", "Auteur(s)", "en", "Author(s)"),
            "abstracts", Map.of("fr", "Résumé(s)", "en", "Abstract(s)"),
            "supervisors", Map.of("fr", "Promoteur(s)", "en", "Supervisor(s)"),
            "title", Map.of("fr", "Titre", "en", "Title")
    );
    private static final Map<String, String> ACCESS_TYPE_LABELS = Map.of(
            "openaccess", "Accès libre | Open access",
            "administrator", "Accès interdit | Closed access",
            "restricted", "Accès restreint UCLouvain | UCLouvain restricted access",
            "embargo", "Accès embargo | Embargo access",
            "unknown", "Accès inconnu | Unknown access"
    );


    // ATTRIBUTES ======================================================================================================
    protected String authorNameField = configService.getProperty(
            "uclouvain.global.metadata.authorname.field", "dc.contributor.author");
    protected String promoterNameField = configService.getProperty(
            "uclouvain.global.metadata.advisorname.field", "dc.contributor.advisor");
    protected String rootDegreeCodeField = configService.getProperty(
            "uclouvain.global.metadata.rootdegreecode.field", "masterthesis.rootdegree.code");
    protected InputStream attachment;

    private final Hasher hasher;
    private final String backendURL = configService.getProperty("dspace.server.url");
    private static final FacultyManagerService facultyManagerService = UCLouvainServiceFactory
            .getInstance()
            .getFacultyManagerService();
    private static final UCLouvainResourcePolicyService uclouvainResourcePolicyService = UCLouvainServiceFactory
            .getInstance()
            .getResourcePolicyService();

    // CONSTRUCTOR =====================================================================================================
    public ThesisAuthorAttestationEmail(Context context, Item item, InputStream attachment)
            throws NoSuchAlgorithmException {
        super(context, item);
        this.attachment = attachment;
        this.hasher = new Hasher(
                configService.getProperty("uclouvain.api.bitstream.download.algorithm", "MD5"),
                configService.getProperty("uclouvain.api.bitstream.download.secret", "")
        );
    }

    /** Configuration used to find the different properties for the email (subject, recipients...) */
    protected String getConfigurationName() {
        return "pdf_attestation";
    }

    /** Get the corresponding template file for the author attestation mail. */
    protected String getTemplatePath() {
        return this.source + "/config/emails/thesis_attestation.author";
    }

    /** Get the string to use as the email subject. */
    protected String buildMailSubject() {
        String authorNames = itemService.getMetadataByMetadataString(item, authorNameField)
                .stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.collectingAndThen(
                    Collectors.joining("; "),
                    joined -> joined.isEmpty() ? "" : " :: " + joined
                ));
        return mailSubject + authorNames;
    }

    /** Get the author email addresses that will be used as recipients. */
    protected List<String> getRecipientAddresses() {
        return itemService.getMetadataByMetadataString(item, authorEmailField)
                .stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
    }

    /** Get the faculty thesis manager corresponding to master thesis as CC recipients. */
    @Override
    protected List<String> getCCAddresses() {
        Set<EPerson> facultyManagers = new HashSet<>();
        for (MetadataValue degreeCode : itemService.getMetadataByMetadataString(item, rootDegreeCodeField)) {
            try {
                facultyManagers.addAll(facultyManagerService.getFacultyManagers(context, degreeCode.getValue()));
            } catch (Exception e) {
                log.error("Error getting faculty managers", e);
            }
        }
        return facultyManagers.stream().map(EPerson::getEmail).collect(Collectors.toList());
    }

    /**
     * Generates a base email version with the given metadata that can be used for both the authors and the promoters.
     *
     * @param email The current email to modify.
     * @throws EmailGenerationException If an error occurs while filling email information.
     */
    protected void generateEmail(Email email) throws EmailGenerationException {
        String entityType = Optional.ofNullable(itemService.getMetadata(item, "dspace.entity.type")).orElse("");
        try {
            email.addArgument(getEmailMetadata("fr"));
            email.addArgument(getEmailMetadata("en"));
            email.addArgument(getFilesDownloadURLs());
            email.addArgument(configService.getProperty("dspace.ui.url") + "/mydspace");
            email.addAttachment(attachment, entityType + "SubmissionAttestation.pdf", "application/pdf");
        } catch (Exception e) {
            throw new EmailGenerationException("An error occurred while filling email informations.", e);
        }
    }

    /**
     * Generates an abstract for the given submission containing the title, the abstract and the authors.
     *
     * @param language the language code to use to build the hashmap.
     * @return The hashmap containing all metadata about the submission to display into the email.
     * @throws ResumeGenerationException if any error occurred during abstract generation
    */
    protected HashMap<String, String> getEmailMetadata(String language) throws ResumeGenerationException {
        HashMap<String, String> emailMetadata = new HashMap<>();
        try {
            emailMetadata.put(getFieldLabel("title", language), getMetadataValue("dc.title", false));
            emailMetadata.put(getFieldLabel("authors", language), getMetadataValue(authorNameField, true));
            emailMetadata.put(getFieldLabel("supervisors", language), getMetadataValue(promoterNameField, true));
            emailMetadata.put(getFieldLabel("abstracts", language), getMetadataValue("dc.description.abstract", true));
            emailMetadata.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
            return emailMetadata;
        } catch (Exception e) {
            throw new ResumeGenerationException("Submission mail generation failed :: " + e.getMessage());
        }
    }

    /**
     * Get i18n label to use about a specific field
     *
     * @param fieldName the field name to check (key into FIELD_LABELS map)
     * @param language the language to check
     * @return the best possible label to use.
     */
    protected String getFieldLabel(String fieldName, String language) {
        // If label doesn't exist, then capitalize the fieldName
        return (FIELD_LABELS.containsKey(fieldName) && FIELD_LABELS.get(fieldName).containsKey(language))
            ? FIELD_LABELS.get(fieldName).get(language)
            : fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1).toLowerCase();
    }

    /**
     * Get string values for a metadata field.
     *
     * @param metadataFieldName the metadata field to check (ex: 'dc_title')
     * @param multiple concatenate multiple values or just return the first one.
     * @return the string representation of the metadata values.
     */
    protected String getMetadataValue(String metadataFieldName, boolean multiple) {
        List<String> metadataValues = itemService.getMetadataByMetadataString(item, metadataFieldName)
                .stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
        if (metadataValues.isEmpty()) {
            return "";
        }
        return (multiple)
            ? String.join("; ", metadataValues)
            : metadataValues.get(0);
    }

    /**
     * Build direct download URLs for item attached files.
     *
     * @return a map where each key is the filename, each value is the download link;
     */
    protected List<String[]> getFilesDownloadURLs() {
        // Get the first supervisor email address; this will be the string used for the hash process.
        String supervisorEmail = itemService.getMetadata(item, supervisorEmailField);
        if (supervisorEmail == null || supervisorEmail.isEmpty()) {
            log.warn("Tried to generate access URLs for the promoters but no email was found.");
            return Collections.emptyList();
        }
        String promoterHash = hasher.processHashAsString(supervisorEmail);

        return item.getBundles("ORIGINAL")
                .stream()
                .findFirst()
                .map(Bundle::getBitstreams).orElse(Collections.emptyList())
                .stream()
                .map(bitstream -> new String[]{
                        bitstream.getName(),
                        getBitstreamAccessStatus(bitstream),
                        getBitstreamDownloadURL(bitstream, promoterHash)
                })
                .collect(Collectors.toList());
    }

    private String getBitstreamDownloadURL(Bitstream bitstream, String hash) {
        return String.format(
                "%s/api/uclouvain/bitstream/%s/content?hash=%s",
                backendURL, bitstream.getID(), hash
        );
    }

    private String getBitstreamAccessStatus(Bitstream bitstream) {
        try {
            List<ResourcePolicy> policies = uclouvainResourcePolicyService.find(context, bitstream);
            ResourcePolicy masterPolicy = uclouvainResourcePolicyService.getMasterPolicy(policies);
            String accessStatus = ACCESS_TYPE_LABELS.getOrDefault(masterPolicy.getRpName(), masterPolicy.getRpName());
            if (masterPolicy.getRpName().equals(EMBARGO)) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                accessStatus += " -- " + formatter.format(masterPolicy.getStartDate());
            }
            return accessStatus;
        } catch (Exception e) {
            return DefaultAccessStatusHelper.UNKNOWN;
        }
    }
}
