package org.dspace.uclouvain.submissionMetadataGenerators.generators;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.submissionMetadataGenerators.exceptions.GeneratorProcessException;

public interface MetadataGenerator {
    String getGeneratorName();
    void process(Context ctx, Item item) throws GeneratorProcessException;
    Boolean canBeProcessed(Context ctx, Item item);
}
