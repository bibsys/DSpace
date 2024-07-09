/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.submissionMetadataGenerators;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.submissionMetadataGenerators.configuration.GenerateMetadataConfigurationService;
import org.dspace.uclouvain.submissionMetadataGenerators.factory.MetadataGeneratorFactory;
import org.dspace.uclouvain.submissionMetadataGenerators.generators.MetadataGenerator;

public class GenerateMetadataServiceImpl implements GenerateMetadataService {

    private final GenerateMetadataConfigurationService generateMetadataConfigurationService =
            MetadataGeneratorFactory.getInstance().getMetadataStepConfigurationService();

    public void executeMetadataGenerationSteps(Context ctx, Item item) throws Exception {
        for (MetadataGenerator mg: generateMetadataConfigurationService.getMetadataGenerators()) {
            try {
                if (mg.canBeProcessed(ctx, item)) {
                    mg.process(ctx, item);
                }
            } catch (Exception e) {
                throw new Exception("Error occurred during metadata generation: " + mg.getGeneratorName(), e);
            }
        }
    }
}
