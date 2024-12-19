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

import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.utils.MetadataUtils;
import org.dspace.uclouvain.pdfAttestationGenerator.configuration.PDFAttestationGeneratorConfiguration;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.HandlerNotFoundException;
import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;
import org.dspace.uclouvain.pdfAttestationGenerator.model.Handler;
import org.springframework.beans.factory.annotation.Autowired;

public class PDFAttestationGeneratorFactoryImpl implements PDFAttestationGeneratorFactory {

    @Autowired
    ItemService itemService;
    @Autowired
    PDFAttestationGeneratorConfiguration pdfAttestationGeneratorConfiguration;

    /** 
     * Returns a handler instance for a dspace item to handle PDF attestation generation.
     * 
     * @param uuid Target DSpace Item's UUID.
     * @return The corresponding handler
     * @throws SQLException if DSpace item doesn't exist.
     * @throws HandlerNotFoundException if no handler found corresponding to Dspace item.
     */
    @Override
    public PDFAttestationGeneratorHandler getHandlerInstance(UUID uuid) throws SQLException, HandlerNotFoundException {
        Context DSpaceContext = new Context();
        Item item = itemService.find(DSpaceContext, uuid);
        if (item == null) {
            throw new SQLException("Object not found");
        }
        String itemType = MetadataUtils.getMetadataFieldValueByFieldId(item.getMetadata(), "dspace_entity_type");

        Handler handler = this.pdfAttestationGeneratorConfiguration.getConfigForItemType(itemType);
        if (handler != null) {
            return handler.handlerClass;
        }
        throw new HandlerNotFoundException(itemType);
    }
}
