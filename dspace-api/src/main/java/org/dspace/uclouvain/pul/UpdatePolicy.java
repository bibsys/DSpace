/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.services.ConfigurationService;

/**
 * How an existing publication is completed from a new ONIX record, field by field. A field listed in none of the
 * three lists is never touched. Authors are not governed by this policy: see {@link PulImporter#update}.
 *
 * @param replace    the ONIX values replace the existing ones
 * @param merge      ONIX values missing from the item are added, existing ones are kept
 * @param addIfEmpty written only when the item has no value at all for the field
 */
public record UpdatePolicy(Set<String> replace, Set<String> merge, Set<String> addIfEmpty) {

    public static final String REPLACE_PROPERTY = "pul.import.update.replace";
    public static final String MERGE_PROPERTY = "pul.import.update.merge";
    public static final String ADD_IF_EMPTY_PROPERTY = "pul.import.update.add-if-empty";

    public enum Behaviour { REPLACE, MERGE, ADD_IF_EMPTY, IGNORE }

    public static UpdatePolicy fromConfiguration(ConfigurationService configurationService) {
        return new UpdatePolicy(
            fields(configurationService, REPLACE_PROPERTY),
            fields(configurationService, MERGE_PROPERTY),
            fields(configurationService, ADD_IF_EMPTY_PROPERTY)
        );
    }

    public Behaviour behaviourFor(String metadataField) {
        // Determine the behavior based on the collection containing the metadata field
        if (replace.contains(metadataField)) {
            return Behaviour.REPLACE;
        } else if (merge.contains(metadataField)) {
            return Behaviour.MERGE;
        } else if (addIfEmpty.contains(metadataField)) {
            return Behaviour.ADD_IF_EMPTY;
        }

        return Behaviour.IGNORE;
    }

    private static Set<String> fields(ConfigurationService configurationService, String property) {
        return Arrays.stream(configurationService.getArrayProperty(property, new String[0]))
            .map(String::trim)
            .filter(field -> !field.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }
}
