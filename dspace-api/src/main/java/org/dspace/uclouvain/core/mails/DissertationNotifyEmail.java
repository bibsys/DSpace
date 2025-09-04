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

public abstract class DissertationNotifyEmail extends AbstractNotifyEmail {

    public DissertationNotifyEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
    }

    @Override
    protected List<String> getCCAddresses() {
        return getAdvisorEmails(context, item);
    }

    protected List<String> getAdvisorEmails(Context context, Item item) {
        List<String> advisorMails = itemService.getMetadataByMetadataString(item, advisorEmailField)
                .stream()
                .map(MetadataValue::getValue)
                .collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            log.debug("Initial CC recipient addresses for notify email are :: " + String.join(", ", advisorMails));
        }
        return advisorMails;
    }
}
