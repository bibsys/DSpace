package org.dspace.uclouvain.submissionMetadataGenerators;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface GenerateMetadataService {
    public void executeMetadataGenerationSteps(Context ctx, Item item) throws Exception;
}
