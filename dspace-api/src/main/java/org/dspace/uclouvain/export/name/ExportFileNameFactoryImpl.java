/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.name;

import java.util.HashMap;
import java.util.Map;

public class ExportFileNameFactoryImpl extends ExportFileNameFactory {
    private ExportFileNameGenerator defaultFileNameGenerator;
    private Map<String, ExportFileNameGenerator> fileNameGenerators = new HashMap<>();

    @Override
    public ExportFileNameGenerator getExportFileNameGenerator(String crosswalkName) {
        return fileNameGenerators.getOrDefault(crosswalkName, defaultFileNameGenerator);
    }

    // SETTERS
    public void setDefaultFileNameGenerator(ExportFileNameGenerator defaultFileNameGenerator) {
        this.defaultFileNameGenerator = defaultFileNameGenerator;
    }

    public void setFileNameGenerators(Map<String, ExportFileNameGenerator> fileNameGenerators) {
        this.fileNameGenerators = fileNameGenerators;
    }
}
