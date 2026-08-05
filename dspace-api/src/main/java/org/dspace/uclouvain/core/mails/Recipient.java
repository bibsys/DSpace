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

    /**
     * Checks if two recipients share at least one identical entry in communicationChannels
     * @param r1: the first recipient to test
     * @param r2: the second recipient to test
     * @return if both recipients share (at least) one communication channel
     */
    public static boolean shareAnyChannel(Recipient r1, Recipient r2) {
        for (Map.Entry<NotificationType, String> entry : r1.communicationChannels().entrySet()) {
            String value2 = r2.communicationChannels().get(entry.getKey());
            if (value2 != null && value2.equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deduplicates a list based on shared channels
     * @param recipients: the list of recipients to deduplicate
     * @return a list of unique recipient, none of them sharing the same channel.
     */
    public static List<Recipient> deduplicateByChannels(List<Recipient> recipients) {
        List<Recipient> result = new ArrayList<>();
        for (Recipient recipient : recipients) {
            boolean isDuplicate = result.stream().anyMatch(existing -> shareAnyChannel(existing, recipient));
            if (!isDuplicate) {
                result.add(recipient);
            }
        }
        return result;
    }
}
