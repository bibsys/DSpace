/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.csl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.undercouch.citeproc.ListItemDataProvider;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;

/**
 * This class allows to override basic {@link ListItemDataProvider} metadata depending on the
 * {@link org.dspace.content.Item} to analyze.
 * If all matching rules are satisfied, then the overriding rules could be applied to {@link ListItemDataProvider}
 * metadata.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class ItemDataProviderOverrideMetadata {

    private Map<String, String> matchingRules;
    private Map<String, String> overrideRules;

    /**
     * Determine if the overriding rules should be applied for a specific {@link org.dspace.content.Item}
     *
     * @param item the item to analyze
     * @return true if all matching rules are validated, false otherwise
     */
    public boolean isMatching(Item item) {
        if (matchingRules == null || matchingRules.isEmpty()) {
            return true;
        }
        ItemService itemService = item.getItemService();
        // All rules must be satisfied (AND condition between different metadata keys)
        return matchingRules.entrySet().stream().allMatch(rule -> {
            String metadataKey = rule.getKey();
            String regex = rule.getValue();
            List<MetadataValue> values = itemService.getMetadataByMetadataString(item, metadataKey);
            if (values == null || values.isEmpty()) {
                return false;
            }
            // At least one metadata value must match the regex (OR condition for multivalued fields)
            return values.stream()
                .map(MetadataValue::getValue)
                .filter(Objects::nonNull) // Ensure we don't call prevent NPE
                .anyMatch(value -> value.matches(regex));
        });
    }

    // GETTER & SETTER =================================================================================================
    public Map<String, String> getMatchingRules() {
        return matchingRules;
    }
    public void setMatchingRules(Map<String, String> matchingRules) {
        this.matchingRules = matchingRules;
    }

    public Map<String, String> getOverrideRules() {
        return overrideRules;
    }
    public void setOverrideRules(Map<String, String> overrideRules) {
        this.overrideRules = overrideRules;
    }
}
