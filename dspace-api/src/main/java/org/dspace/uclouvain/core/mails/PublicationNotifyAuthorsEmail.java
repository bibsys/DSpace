/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import static org.dspace.uclouvain.constants.AccessConditions.EMBARGO;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.access.status.DefaultAccessStatusHelper;
import org.dspace.access.status.factory.AccessStatusServiceFactory;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;
import org.dspace.uclouvain.exceptions.EmailGenerationException;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

/**
 * Mail to notify the authors of a publication that it has been submitted.
 */
public class PublicationNotifyAuthorsEmail extends GenericPublicationEmail {

    protected final List<String> fieldsToExpose = Arrays.asList(getConfigurationAttributes("metadata"));
    protected AccessStatusService accessStatusService =
        AccessStatusServiceFactory.getInstance().getAccessStatusService();
    private UCLouvainResourcePolicyService uclouvainResourcePolicyService =
        UCLouvainServiceFactory
            .getInstance()
            .getResourcePolicyService();

    private static final Map<String, String> ACCESS_TYPE_LABELS = Map.of(
        "openaccess", "Accès libre | Open access",
        "administrator", "Accès interdit | Closed access",
        "restricted", "Accès restreint UCLouvain | UCLouvain restricted access",
        "embargo", "Accès embargo | Embargo access",
        "unknown", "Accès inconnu | Unknown access"
    );

    public PublicationNotifyAuthorsEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
    }

    @Override
    protected String getConfigurationName() {
        return "notify_authors";
    }

    @Override
    protected String getTemplatePath() {
        return this.source + "/config/emails/publication_notify_authors";
    }

    @Override
    protected String buildMailSubject() {
        return mailSubject;
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
        if (log.isDebugEnabled()) {
            log.debug("Initial TO recipient addresses for change request are :: " + String.join(", ", recipients));
        }
        return recipients;
    }

    @Override
    protected List<String> getCCAddresses() {
        // TODO: Maybe add promoter as CC ??
        return new ArrayList<>();
    }

    @Override
    protected void generateEmail(Email email, Item item) throws EmailGenerationException {
        try {
            email.addArgument(mailMetadataParserService.parseMetadata(context, item, fieldsToExpose, "fr"));
            email.addArgument(mailMetadataParserService.parseMetadata(context, item, fieldsToExpose, "en"));
            email.addArgument(getAttachedFiles(context, item));
        } catch (Exception e) {
            throw new EmailGenerationException("An error occurred while filling email informations", e);
        }
    }

    protected List<String[]> getAttachedFiles(Context context, Item item) {
        return item.getBundles(Constants.CONTENT_BUNDLE_NAME)
                .stream()
                .findFirst()
                .map(Bundle::getBitstreams).orElse(Collections.emptyList())
                .stream()
                .map(bitstream -> new String[]{
                        bitstream.getName(),
                        getBitstreamAccessStatus(context, bitstream)
                })
                .collect(Collectors.toList());
    }

    private String getBitstreamAccessStatus(Context context, Bitstream bitstream) {
        try {
            List<ResourcePolicy> policies = uclouvainResourcePolicyService.find(context, bitstream);
            ResourcePolicy masterPolicy = uclouvainResourcePolicyService.getMasterPolicy(policies);
            String accessStatus = ACCESS_TYPE_LABELS.getOrDefault(masterPolicy.getRpName(), masterPolicy.getRpName());
            if (masterPolicy.getRpName().equals(EMBARGO)) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                accessStatus += " -- " + formatter.format(masterPolicy.getStartDate());
            }
            return accessStatus;
        } catch (SQLException e) {
            return DefaultAccessStatusHelper.UNKNOWN;
        }
    }
}
