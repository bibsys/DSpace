package org.dspace.uclouvain.submissionMetadataGenerators.configuration;


import java.util.List;

import org.dspace.uclouvain.submissionMetadataGenerators.generators.MetadataGenerator;

/**
 * Basic configuration service for metadata generators.
 * Configured via beans.
 */
public class GenerateMetadataConfigurationService {
    private List<MetadataGenerator> metadataGenerators;

    // Getters && Setters
    public void setMetadataGenerators(List<MetadataGenerator> metadataGenerators) {
        this.metadataGenerators = metadataGenerators;
    }

    public List<MetadataGenerator> getMetadataGenerators() {
        return this.metadataGenerators;
    }
}
