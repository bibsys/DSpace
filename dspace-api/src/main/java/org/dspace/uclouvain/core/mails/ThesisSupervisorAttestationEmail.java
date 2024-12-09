/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Main class to send an email for the submission attestation to the promoters of the item.
 * This mail is sent when someone makes a new submission, and it enters the workflow validation system.
 * This class extends {@link ThesisAuthorAttestationEmail} and adds the generation of file access links for promoters.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ThesisSupervisorAttestationEmail extends ThesisAuthorAttestationEmail {

    public ThesisSupervisorAttestationEmail(Context context, Item item, InputStream attachment) {
        super(context, item, attachment);
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
        return itemService.getMetadataByMetadataString(item, supervisorEmailField)
                .stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
    }
}
