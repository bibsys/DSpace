package org.dspace.uclouvain.submissionMetadataGenerators;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.submissionMetadataGenerators.configuration.GenerateMetadataConfigurationService;
import org.dspace.uclouvain.submissionMetadataGenerators.factory.MetadataGeneratorFactory;
import org.dspace.uclouvain.submissionMetadataGenerators.generators.MetadataGenerator;

public class GenerateMetadataServiceImpl implements GenerateMetadataService {

    private GenerateMetadataConfigurationService generateMetadataConfigurationService = MetadataGeneratorFactory.getInstance().getMetadataStepConfigurationService();

    public void executeMetadataGenerationSteps(Context ctx, Item item) throws Exception {
        for (MetadataGenerator mg: generateMetadataConfigurationService.getMetadataGenerators()) {
            try {
                mg.process(ctx, item);
            }
            catch (Exception e){
                throw new Exception("An error occurred with the following metadata generator: " + mg.getGeneratorName(), e);
            }
        };
    }
}
