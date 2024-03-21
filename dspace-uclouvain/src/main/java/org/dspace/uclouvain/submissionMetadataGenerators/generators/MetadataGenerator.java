package org.dspace.uclouvain.submissionMetadataGenerators.generators;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.submissionMetadataGenerators.exceptions.GeneratorProcessException;

public interface MetadataGenerator {
    public String getGeneratorName();
    public void process(Context ctx, Item item) throws GeneratorProcessException;
    public Boolean canBeProcessed(Context ctx, Item item);
}
