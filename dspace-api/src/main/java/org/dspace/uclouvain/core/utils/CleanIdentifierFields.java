/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;

/**
 * The identifier metadata fields indexed under a canonical form, as declared by
 * {@code uclouvain.indexing.clean-identifiers = <metadata field>:<IdentifierNormalizer>, ...}.
 * <p>
 * This is the single reading of that property: the Solr indexing plugin uses it to write the
 * {@code <field>.clean_keyword} values, and the search side uses it to normalize a searched value with the very
 * same {@link IdentifierNormalizer} before querying that Solr field.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public final class CleanIdentifierFields {

    public static final String PROPERTY = "uclouvain.indexing.clean-identifiers";
    private static final String SOLR_SUFFIX = ".clean_keyword";
    private static final Logger log = LogManager.getLogger(CleanIdentifierFields.class);

    private final Map<String, IdentifierNormalizer> normalizers = new LinkedHashMap<>();

    public CleanIdentifierFields(ConfigurationService configurationService) {
        for (String entry : configurationService.getArrayProperty(PROPERTY, new String[0])) {
            String[] parts = entry.split(":", 2);
            Optional<IdentifierNormalizer> normalizer = parts.length == 2
                ? IdentifierNormalizer.of(parts[1])
                : Optional.empty();
            if (normalizer.isEmpty()) {
                log.warn("Ignoring '{}' entry [{}]: expected '<metadata field>:<one of {}>'",
                    PROPERTY, entry, List.of(IdentifierNormalizer.values()));
                continue;
            }
            normalizers.put(parts[0].trim(), normalizer.get());
        }
    }

    /** @return the configured metadata fields and their normalizer, in configuration order. */
    public Map<String, IdentifierNormalizer> asMap() {
        return Collections.unmodifiableMap(normalizers);
    }

    /**
     * @param metadataField a metadata field name, e.g. {@code dc.identifier.isbn}.
     * @return its normalizer, or empty if the field is not configured (hence not indexed under a clean form).
     */
    public Optional<IdentifierNormalizer> normalizerFor(String metadataField) {
        return Optional.ofNullable(normalizers.get(metadataField));
    }

    /**
     * @param metadataField a configured metadata field name.
     * @return the Solr field holding its canonical forms.
     */
    public static String solrField(String metadataField) {
        return metadataField + SOLR_SUFFIX;
    }
}
