/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.submissionMetadataGenerators.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.submissionMetadataGenerators.configuration.GenerateMetadataConfigurationService;

public interface MetadataGeneratorFactory {


    static MetadataGeneratorFactory getInstance() {
        return DSpaceServicesFactory
                .getInstance()
                .getServiceManager()
                .getServiceByName("metadataGeneratorFactory", MetadataGeneratorFactory.class);
    }

    GenerateMetadataConfigurationService getMetadataStepConfigurationService();
}
