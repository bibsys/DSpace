/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.submissionMetadataGenerators.configuration;

import java.util.List;

import org.dspace.uclouvain.submissionMetadataGenerators.generators.MetadataGenerator;

/**
 * Basic configuration service for metadata generators.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be):
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
