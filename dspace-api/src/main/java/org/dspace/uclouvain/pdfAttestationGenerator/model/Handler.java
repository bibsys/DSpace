/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
