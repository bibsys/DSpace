/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.uclouvain.export.name.ExportFileNameFactory;
import org.dspace.uclouvain.export.name.ExportFileNameGenerator;

/**
 * Define interface to get file name to showed on download view by configuration.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public interface FileNameDisseminator {

    /**
     * Get the basic filename declared by the crosswalk.
     *
     * @return the filename declared by the crosswalk
     */
    public String getFileName();

    /**
     * Generate an export filename using the configured filename generator.
     * <p>
     * This default implementation resolves the global {@link ExportFileNameGenerator}
     * from the DSpace service manager and delegates filename generation to it.
     * If no generator is available, the original crosswalk filename is used as a fallback.
     *
     * @param crosswalkName the crosswalk identifier used for filename generation
     * @param attributes a map of attributes available to the filename template
     * @return the generated filename
     */
    default String getFileName(String crosswalkName, Map<String, String> attributes) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        if (!(this instanceof StreamDisseminationCrosswalk)) {
            return getFileName();
        }

        ExportFileNameGenerator generator = ExportFileNameFactory
            .getInstance()
            .getExportFileNameGenerator(crosswalkName);
        if (generator != null) {
            return generator.generateFileName(getFileName(), attributes);
        }

        // Fallback to the original crosswalk-defined filename when no generator is configured.
        return getFileName();
    }
}