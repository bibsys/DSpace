/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import static org.dspace.core.Constants.CONTENT_BUNDLE_NAME;
import static org.dspace.uclouvain.constants.AccessConditions.EMBARGO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.dspace.access.status.DefaultAccessStatusHelper;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.core.directLink.ThesisSupervisorDirectLinkGenerator;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;
import org.dspace.uclouvain.exceptions.EmailGenerationException;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.factory.PDFAttestationGeneratorFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;
import org.dspace.uclouvain.services.DirectLinkService;
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

    private static final FacultyManagerService facultyManagerService = UCLouvainServiceFactory
            .getInstance()
            .getFacultyManagerService();
    private static final DirectLinkService uclouvainDirectLinkService = UCLouvainServiceFactory
            .getInstance()
            .getDirectLinkService();
    private static final UCLouvainResourcePolicyService uclouvainResourcePolicyService = UCLouvainServiceFactory
            .getInstance()
            .getResourcePolicyService();

    protected final List<String> fieldsToExpose = Arrays.asList(getConfigurationAttributes("metadata"));

    // METHODS =========================================================================================================
    public ThesisAuthorAttestationEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
    }

    public boolean isValidForItem(Context context, Item item) {
        return super.isValidForItem(context, item) && hasAnyAuthor(item);
    }

    private boolean hasAnyAuthor(Item item) {
        List<String> authors = getAuthorAdresses(item);
        return authors != null && !authors.isEmpty();
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
        return getAuthorAdresses(item);
    }

    private List<String> getAuthorAdresses(Item item) {
        List<String> authors = itemService.getMetadataByMetadataString(item, authorEmailField)
                .stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            log.debug("Initial TO recipient addresses for author attestation are :: " + String.join(", ", authors));
        }
        return authors;
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
        List<String> recipients = facultyManagers.stream().map(EPerson::getEmail).collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            log.debug("Initial CC recipient addresses are :: " + String.join(", ", recipients));
        }
        return recipients;
    }

    /**
     * Generates a base email version with the given metadata that can be used for both the authors and the promoters.
     *
     * @param email The current email to modify.
     * @param item The item used to generate the email.
     * @throws EmailGenerationException If an error occurs while filling email information.
     */
    protected void generateEmail(Email email, Item item) throws EmailGenerationException {
        String entityType = Optional.ofNullable(itemService.getMetadata(item, "dspace.entity.type")).orElse("");
        UUID uuid = item.getID();
        try {
            PDFAttestationGeneratorHandler handler = PDFAttestationGeneratorFactory
                    .getInstance()
                    .getHandlerInstance(uuid);
            ByteArrayInputStream pdfAttestation = new ByteArrayInputStream(
                IOUtils.toByteArray(handler.getAttestationAsInputStream(uuid))
            );

            email.addArgument(mailMetadataParserService.parseMetadata(context, item, fieldsToExpose,"fr"));
            email.addArgument(mailMetadataParserService.parseMetadata(context, item, fieldsToExpose,"en"));
            email.addArgument(getFilesDownloadURLs());
            email.addArgument(configService.getProperty("dspace.ui.url") + "/mydspace");
            email.addAttachment(pdfAttestation, entityType + "SubmissionAttestation.pdf", "application/pdf");
        } catch (Exception e) {
            throw new EmailGenerationException("An error occurred while filling email informations.", e);
        }
    }

    /**
     * Build direct download URLs for item attached files.
     *
     * @return a map where each key is the filename, each value is the download link;
     */
    protected List<String[]> getFilesDownloadURLs() {
        List<String[]> urls = new ArrayList<>();
        try {
            String linkType = ThesisSupervisorDirectLinkGenerator.LINK_TYPE;
            List<Bitstream> bitstreams = item.getBundles(CONTENT_BUNDLE_NAME)
                    .stream()
                    .flatMap(bundle -> bundle.getBitstreams().stream())
                    .toList();
            for (Bitstream bitstream : bitstreams) {
                urls.add(new String[] {
                    bitstream.getName(),
                    getBitstreamAccessStatus(bitstream),
                    uclouvainDirectLinkService.buildURL(context, bitstream, linkType, Collections.emptyMap())
                });
            }
        } catch (Exception e) {
            log.error("Unable to generate direct download URLs for supervisors :: " + e.getMessage());
            return Collections.emptyList();
        }
        return urls;
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