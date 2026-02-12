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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dspace.access.status.DefaultAccessStatusHelper;
import org.dspace.access.status.factory.AccessStatusServiceFactory;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.plugins.UCLouvainAccessStatusHelper;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

public abstract class AbstractNotifyEmail extends GenericPublicationEmail {
    protected AccessStatusService accessStatusService = AccessStatusServiceFactory
        .getInstance()
        .getAccessStatusService();
    protected UCLouvainResourcePolicyService uclouvainResourcePolicyService = UCLouvainServiceFactory
        .getInstance()
        .getResourcePolicyService();
    private HandleService handleService = HandleServiceFactory
        .getInstance()
        .getHandleService();

    protected static final Map<String, String> ACCESS_TYPE_LABELS = Map.of(
        UCLouvainAccessStatusHelper.OPEN_ACCESS, "Accès libre | Open access",
        UCLouvainAccessStatusHelper.ADMINISTRATOR, "Accès interdit | Closed access",
        UCLouvainAccessStatusHelper.RESTRICTED, "Accès restreint UCLouvain | UCLouvain restricted access",
        UCLouvainAccessStatusHelper.EMBARGO, "Accès embargo | Embargo access",
        UCLouvainAccessStatusHelper.UNKNOWN, "Accès inconnu | Unknown access"
    );

    public AbstractNotifyEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
    }

    @Override
    protected String buildMailSubject() {
        return mailSubject;
    }

    @Override
    protected List<String> getRecipientAddresses() {
        // Send email on both private and official addresses.
        List<String> recipients = publication.getAuthorsEmails(true, true);
        String submitterEmail = item.getSubmitter().getEmail();
        if (!recipients.contains(submitterEmail)) {
            recipients.add(submitterEmail);
        }
        if (log.isDebugEnabled()) {
            log.debug("Initial TO recipient addresses for notify email are :: {}", String.join(", ", recipients));
        }
        return recipients;
    }

    protected String getHandle(Context context, Item item) throws SQLException {
        return Optional.ofNullable(handleService.findHandle(context, item))
            .map(handleService::getCanonicalForm)
            .orElse(null);
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
