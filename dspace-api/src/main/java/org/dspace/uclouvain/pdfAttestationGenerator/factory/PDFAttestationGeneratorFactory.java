/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pdfAttestationGenerator.factory;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.HandlerNotFoundException;
import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;

public interface PDFAttestationGeneratorFactory {
    PDFAttestationGeneratorHandler getHandlerInstance(UUID uuid) throws SQLException, HandlerNotFoundException;

    static PDFAttestationGeneratorFactory getInstance() {
        return DSpaceServicesFactory
            .getInstance()
            .getServiceManager()
            .getServiceByName("pdfAttestationGeneratorFactory", PDFAttestationGeneratorFactory.class);
    }
}
