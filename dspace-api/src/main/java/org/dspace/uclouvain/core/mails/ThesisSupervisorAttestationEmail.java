/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;

/**
 * Main class to send an email for the submission attestation to the promoters of the item.
 * This mail is sent when someone makes a new submission, and it enters the workflow validation system.
 * This class extends {@link ThesisAuthorAttestationEmail} and adds the generation of file access links for promoters.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ThesisSupervisorAttestationEmail extends ThesisAuthorAttestationEmail {
    public ThesisSupervisorAttestationEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
    }

    public boolean isValidForItem(Context context, Item item) {
        return super.isValidForItem(context, item) && hasAnyPromoter(item);
    }

    /**
     * Check if the item contains at least one promoter address.
     * @param item The item to check promoters of.
     * @return True if any promoter in the item metadata, false otherwise.
     */
    private boolean hasAnyPromoter(Item item) {
        List<String> promoterEmails = getPromoterAdresses(item);
        return promoterEmails != null && !promoterEmails.isEmpty();
    }

    /**
     * Get the corresponding template file for the promoter attestation mail.
     */
    @Override
    protected String getTemplatePath() {
        return this.source + "/config/emails/thesis_attestation.supervisor";
    }

    /**
     * Get the promoter email addresses that will be used as recipients.
     *
     * @return the recipient addresses list.
     */
    @Override
    protected List<String> getRecipientAddresses() {
        return getPromoterAdresses(item);
    }

    private List<String> getPromoterAdresses(Item item) {
        List<String> promoters = itemService.getMetadataByMetadataString(item, supervisorEmailField)
                .stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            log.debug("Initial TO recipient addresses for promoter attestation are :: " + String.join(", ", promoters));
        }
        return promoters;
    }
}
