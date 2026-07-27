/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.name;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Default export filename generator used by crosswalks.
 * This filename generator appends the current date to the base filename.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class DateExportFileNameGenerator extends ExportFileNameGenerator {

    final String FILE_NAME_TEMPLATE = "%s_%s.%s";
    final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Generate a filename for an export result using the configured template.
     *
     * @param baseFileName the base filename for the export
     * @param attributes a map of template attributes for filename generation
     * @return the generated filename with safe characters and extension preservation
     */
    @Override
    public String generateFileName(
        String baseFileName,
        Map<String, String> attributes
    ) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        Pair<String, String> fileNameParts = decomposeFileName(baseFileName);

        return FILE_NAME_TEMPLATE.formatted(
            fileNameParts.getLeft(),
            LocalDate.now().format(DATE_FORMAT),
            fileNameParts.getRight()
        );
    }
}
