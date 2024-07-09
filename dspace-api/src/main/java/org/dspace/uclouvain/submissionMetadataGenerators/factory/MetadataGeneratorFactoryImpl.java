/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
