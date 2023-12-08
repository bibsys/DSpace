package org.dspace.uclouvain.pdfAttestationGenerator.factory;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.HandlerNotFoundException;
import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;

public interface PDFAttestationGeneratorFactory {
    public PDFAttestationGeneratorHandler getHandlerInstance(UUID uuid) throws SQLException, HandlerNotFoundException;

    static PDFAttestationGeneratorFactory getInstance() {
        return DSpaceServicesFactory
            .getInstance()
            .getServiceManager()
            .getServiceByName("pdfAttestationGeneratorFactory", PDFAttestationGeneratorFactory.class);
    }
}
