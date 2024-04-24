package org.dspace.uclouvain.pdfAttestationGenerator.configuration;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.uclouvain.pdfAttestationGenerator.model.Handler;

public class PDFAttestationGeneratorConfiguration {

    private List<Handler> handlersConfiguration;

    /** 
     * Search for a specific configuration based on the itemType.
     * 
     * @param itemType: The type of the item to search for the config.
     * @return Handler object or null if not found.
     */
    public Handler getConfigForItemType(String itemType) {
        for (Handler handler: this.handlersConfiguration) {
            if (handler.itemType.equals(itemType)) return handler;
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
            // Could maybe be changed to 'Handler::getItemType' ??? Not sure
            .map(handler -> handler.getItemType())
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
