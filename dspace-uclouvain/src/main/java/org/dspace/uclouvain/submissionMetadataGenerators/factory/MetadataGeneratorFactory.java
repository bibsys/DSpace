package org.dspace.uclouvain.submissionMetadataGenerators.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.submissionMetadataGenerators.configuration.GenerateMetadataConfigurationService;

public interface MetadataGeneratorFactory {
    public GenerateMetadataConfigurationService getMetadataStepConfigurationService();

    static MetadataGeneratorFactory getInstance() {
        return DSpaceServicesFactory
            .getInstance()
            .getServiceManager()
            .getServiceByName("metadataGeneratorFactory", MetadataGeneratorFactory.class);
    }
}
