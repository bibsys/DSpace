package org.dspace.uclouvain.submissionMetadataGenerators.factory;

import org.dspace.uclouvain.submissionMetadataGenerators.configuration.GenerateMetadataConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class MetadataGeneratorFactoryImpl implements MetadataGeneratorFactory {

    @Autowired
    private GenerateMetadataConfigurationService generateMetadataStepConfigurationService;

    public GenerateMetadataConfigurationService getMetadataStepConfigurationService() {
        return generateMetadataStepConfigurationService;
    }
}   
