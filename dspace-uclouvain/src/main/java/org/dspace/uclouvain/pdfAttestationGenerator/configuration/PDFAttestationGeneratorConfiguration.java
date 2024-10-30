/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pdfAttestationGenerator.configuration;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.uclouvain.pdfAttestationGenerator.model.Handler;

public class PDFAttestationGeneratorConfiguration {

    private List<Handler> handlersConfiguration;

    /** 
     * Search for a specific configuration based on the itemType.
     * 
     * @param itemType The type of the item to search for the config.
     * @return Handler object or null if not found.
     */
    public Handler getConfigForItemType(String itemType) {
        for (Handler handler: this.handlersConfiguration) {
            if (handler.itemType.equals(itemType)) {
                return handler;
            }
        }
        return null;
    }

    /**
     * Get a list of all handled item types for PDF attestation generation.
     * 
     * @return List of all handled item types.
     */
    public List<String> getAllHandledTypes() {
        return this.handlersConfiguration
            .stream()
            .map(Handler::getItemType)
            .distinct()
            .collect(Collectors.toList());
    }

    // GETTERS && SETTERS
    public List<Handler> getHandlersConfiguration() {
        return this.handlersConfiguration;
    }

    public void setHandlersConfiguration(List<Handler> handlersConfiguration) {
        this.handlersConfiguration = handlersConfiguration;
    }
}
