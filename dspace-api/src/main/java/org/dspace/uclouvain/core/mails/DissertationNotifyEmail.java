/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;

public abstract class DissertationNotifyEmail extends AbstractNotifyEmail {

    public DissertationNotifyEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
    }

    @Override
    protected List<String> getCCAddresses() {
        List<String> ccAddresses = new ArrayList<>();
        ccAddresses.addAll(getAdvisorEmails(context, item));
        ccAddresses.addAll(getManagerEmails(context, item));
        return ccAddresses.stream().distinct().toList();
    }

    /**
     * Get all advisors emails from the item metadata and return them as a list of strings.
     * @param context The current DSpace application context.
     * @param item The item to get the advisor emails from.
     * @return A list of strings containing all the advisor emails found in the item metadata.
     */
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

    /**
     * Try to get all managers of the item and return their email addresses.
     * @param context The current DSpace application context.
     * @param item The item to get managers of.
     * @return All the email addresses of the managers of the item.
     * If any exception occurs during the retrieval of the managers or their emails, an empty list is returned.
     */
    protected List<String> getManagerEmails(Context context, Item item) {
        try {
            return ItemUtils.getManagersOfItem(context, item).stream()
                .map(manager -> manager.getEmail())
                .filter(email -> email != null && !email.isEmpty())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not retrieve manager emails for item with id " + item.getID(), e);
            return List.of();
        }
    }
}
