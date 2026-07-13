/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.util.Map;

import org.dspace.uclouvain.core.NotificationType;

/**
 * Represents a recipient with possible multiple communication channels
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public record Recipient(
    String name,
    Map<NotificationType, String> communicationChannels
) {

    public Recipient {
        // Ensure communication channel is always a valid Map (never null)
        communicationChannels = (communicationChannels != null)
            ? Map.copyOf(communicationChannels)
            : Map.of();
    }

    public String get(NotificationType key) {
        return communicationChannels.get(key);
    }

    public boolean has(NotificationType key) {
        return communicationChannels.containsKey(key);
    }
}