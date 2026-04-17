/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.core.model.publication.DissertationPublication;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;

public abstract class DissertationNotifyEmail extends AbstractNotifyEmail {
    protected List<String> forcedManagers;

    public DissertationNotifyEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
        forcedManagers = Arrays.asList(getConfigurationAttributes("additional-cc-addresses"));
    }

    @Override
    protected List<String> getCCAddresses() {
        List<String> ccAddresses = new ArrayList<>();
        ccAddresses.addAll(getSupervisorEmails(publication));
        ccAddresses.addAll(getManagerEmails(context, item));
        ccAddresses.addAll(forcedManagers);
        return ccAddresses.stream().distinct().toList();
    }

    /**
     * Get all advisors emails from the item metadata and return them as a list of strings.
     * @param publication The item to get the advisor emails from.
     * @return A list of strings containing all the advisor emails found in the item metadata.
     */
    protected List<String> getSupervisorEmails(Publication publication) {
        if (publication instanceof DissertationPublication thesis) {
            List<String> supervisorEmails = thesis.getSupervisorEmails();
            if (log.isDebugEnabled()) {
                String emails = String.join(", ", supervisorEmails);
                log.debug("Initial CC recipient addresses for notify email are :: {}", emails);
            }
            return supervisorEmails;
        }
        return Collections.emptyList();
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
                .map(EPerson::getEmail)
                .filter(email -> email != null && !email.isEmpty())
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not retrieve manager emails for item with id {}", item.getID(), e);
            return List.of();
        }
    }
}
