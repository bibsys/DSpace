/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.name;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

/**
 * A filename generator for author specific exports.
 * This file name generator will append the author's name in the filename.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class AuthorExportFileNameGenerator extends DateExportFileNameGenerator {

    final String AUTHOR_FILE_NAME_TEMPLATE = "%s%s.%s";

    /**
     * Generate a filename for an export result using the configured template.
     *
     * @param baseFileName the base filename for the export
     * @param attributes a map of template attributes for filename generation
     * @return the generated filename with safe characters and extension preservation
     */
    @Override
    public String generateFileName(String baseFileName, Map<String, String> attributes) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        String authorName = attributes.getOrDefault("authorName", "");
        if (!authorName.isEmpty()) {
            authorName = "_" + authorName;
        }

        Pair<String, String> fileNameParts = decomposeFileName(baseFileName);
        String finalFileName = AUTHOR_FILE_NAME_TEMPLATE.formatted(
            fileNameParts.getLeft(),
            formatAuthorName(authorName),
            fileNameParts.getRight()
        );
        return super.generateFileName(finalFileName, attributes);
    }

    /**
     * Format the author's name to be safe for use in filenames by removing accents,
     * replacing spaces and special characters with underscores, and converting to lowercase.
     * 
     * @param name The author's name to format
     * @return A formatted string suitable for use in filenames
     */
    private String formatAuthorName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        // 1. Decompose Unicode characters to separate base letters from accents
        String cleaned = Normalizer.normalize(name, Normalizer.Form.NFD);
        // 2. Remove all diacritical marks (accents, umlauts, etc.)
        cleaned = cleaned.replaceAll("\\p{M}", "");
        // 3. Replace comma (and any following spaces) with an underscore
        cleaned = cleaned.replaceAll(",\\s*", "_");
        // 4. Replace remaining spaces and hyphens (compound names) with underscores
        cleaned = cleaned.replaceAll("[\\s\\-]+", "_");
        // 5. Convert the entire string to lowercase
        return cleaned.toLowerCase();
    }
}
