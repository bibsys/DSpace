/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.name;

import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Factory to retrieve the correct {@link ExportFileNameGenerator} based on the given crosswalk name.
 * 
 * @author Michael Pourbaix <michael.pourbaix@uclouvain.be>
 */
public abstract class ExportFileNameFactory {
    /**
     * Get the {@link ExportFileNameGenerator} for the given crosswalk name.
     * If no generator is found then return the default configured generator.
     * @param crosswalkName the crosswalk name to get the generator for
     * @return the {@link ExportFileNameGenerator} for the given crosswalk name
     */
    public abstract ExportFileNameGenerator getExportFileNameGenerator(String crosswalkName);

    public static ExportFileNameFactory getInstance() {
        return DSpaceServicesFactory
                .getInstance()
                .getServiceManager()
                .getServiceByName("exportFileNameFactory", ExportFileNameFactory.class);
    }
}
