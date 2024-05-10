package org.dspace.uclouvain.pdfAttestationGenerator.rest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.fop.apps.FOPException;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.pdfAttestationGenerator.AttestationAuthorizationService;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.HandlerNotFoundException;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.PDFGenerationException;
import org.dspace.uclouvain.pdfAttestationGenerator.factory.PDFAttestationGeneratorFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.xml.sax.SAXException;

@RestController
@RequestMapping("/api/uclouvain/item/{uuid}/attestation")
public class PDFAttestationGeneratorRestController {

    @Autowired
    private ItemService itemService;

    @Autowired
    private AttestationAuthorizationService attestationAuthorizationService;

    /** 
     * Generates and returns a PDF attestation with a template depending on the targeted DSpace object type
     * 
     * @param uuid: The uuid of the target DSpace Item from the request
     */
    @RequestMapping(method = RequestMethod.GET)
    public void attestation(
        HttpServletResponse response,
        HttpServletRequest request,
        @PathVariable UUID uuid
    ) throws FileNotFoundException, IOException, FOPException, TransformerConfigurationException, TransformerException, SAXException, SQLException {
        try {
            if(this.checkAuthorization(request, uuid)) {
                // If the authorization check passes, handler cannot be null.
                // See why in 'AttestationAuthorizationService.isItemValidForAttestation'
                PDFAttestationGeneratorHandler handler = PDFAttestationGeneratorFactory.getInstance().getHandlerInstance(uuid);
                try {
                    response.setContentType("application/pdf");
                    handler.getAttestation(response.getOutputStream(), uuid);
                    response.flushBuffer();
                } catch (PDFGenerationException e) {
                    response.sendError(500, "An error occurred while generating the attestation");
                }
            }
            else {
                response.sendError(401, "You are not authorize to access this resource");
            }
        } catch (SQLException e){
            response.sendError(404, "Object not found");
        } 
        catch (HandlerNotFoundException e) {
            response.sendError(404, "No handler configured for this type of item");
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Boolean checkAuthorization(HttpServletRequest request, UUID uuid) throws SQLException {
        Context ctx = ContextUtil.obtainContext(request);
        Item dsItem = itemService.find(ctx, uuid);
        if (dsItem == null) return false;

        return attestationAuthorizationService.isItemValidForAttestation(dsItem, ctx) 
            && attestationAuthorizationService.isUserAuthorized(dsItem, ctx);
    }
}