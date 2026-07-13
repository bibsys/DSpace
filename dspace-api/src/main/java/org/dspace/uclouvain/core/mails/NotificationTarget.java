/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.dspace.uclouvain.core.NotificationType;

/**
 * Wraps a Recipient and a specific communication channel to group notifications.
 * Two targets are equal if they share the exact same contact address for the chosen channel.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public record NotificationTarget(
    Recipient recipient,
    NotificationType channel
) {
    public NotificationTarget {
        Objects.requireNonNull(recipient, "Recipient cannot be null");
        Objects.requireNonNull(channel, "Channel cannot be null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NotificationTarget other)) {
            return false;
        }
        String thisContact = recipient.get(channel);
        String thatContact = other.recipient.get(channel);
        // They are equal if their contact for this channel are identical and not blank
        return StringUtils.isNotBlank(thisContact) && Objects.equals(thisContact, thatContact);
    }

    @Override
    public int hashCode() {
        String contact = recipient.get(channel);
        return StringUtils.isNotBlank(contact) ? contact.hashCode() : 0;
    }
}