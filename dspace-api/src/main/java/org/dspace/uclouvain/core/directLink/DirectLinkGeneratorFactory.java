/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.directLink;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Factory use to retrieve a direct link generator based on its type
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Component
public class DirectLinkGeneratorFactory {

    private Map<String, DirectLinkGenerator> generators = new HashMap<>();

    public DirectLinkGenerator getGenerator(String linkType) {
        return Optional
            .ofNullable(generators.get(linkType))
            .orElseThrow(() -> new IllegalArgumentException("No generator found for '" + linkType + "' key"));
    }

    public void setGenerators(Map<String, DirectLinkGenerator> generators) {
        this.generators = generators;
    }
}
