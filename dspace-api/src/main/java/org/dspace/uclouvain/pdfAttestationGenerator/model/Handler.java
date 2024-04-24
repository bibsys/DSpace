package org.dspace.uclouvain.pdfAttestationGenerator.model;

import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;

public class Handler {
    public String itemType;
    public PDFAttestationGeneratorHandler handlerClass;

    // GETTERS && SETTERS
    public String getItemType() {
        return this.itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public PDFAttestationGeneratorHandler getHandlerClass() {
        return this.handlerClass;
    }

    public void setHandlerClass(PDFAttestationGeneratorHandler handlerClass) {
        this.handlerClass = handlerClass;
    }
}
