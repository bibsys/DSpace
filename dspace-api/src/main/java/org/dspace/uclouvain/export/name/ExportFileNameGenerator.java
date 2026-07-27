/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.name;

import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public abstract class ExportFileNameGenerator {

    /**
     * Generate a filename for an export result.
     *
     * @param baseFileName The base filename for the export.
     * @param attributes A map of custom attributes used by filename templates.
     * @return The generated filename.
     */
    public abstract String generateFileName(
        String baseFileName,
        Map<String, String> attributes
    );

    /**
     * Decompose a filename into its prefix and extension.
     * @param fileName The filename to decompose.
     * @return A pair containing the prefix and extension of the filename.
     *         If the filename has no extension, the second element of the pair will be null.
     */
    Pair<String, String> decomposeFileName(String fileName) {
        // This should not include the dot
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return Pair.of(fileName, null);
        }
        return Pair.of(fileName.substring(0, lastDotIndex), fileName.substring(lastDotIndex + 1));
    }
}
