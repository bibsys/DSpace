/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.mails.ThesisAuthorAttestationEmail;
import org.dspace.uclouvain.core.mails.ThesisSupervisorAttestationEmail;
import org.dspace.uclouvain.pdfAttestationGenerator.AttestationAuthorizationService;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.HandlerNotFoundException;
import org.dspace.uclouvain.pdfAttestationGenerator.exceptions.PDFGenerationException;
import org.dspace.uclouvain.pdfAttestationGenerator.factory.PDFAttestationGeneratorFactory;
import org.dspace.uclouvain.pdfAttestationGenerator.handlers.PDFAttestationGeneratorHandler;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/uclouvain/item/{uuid}/attestation")
public class AttestationRestController {

    @Autowired
    private ItemService itemService;
    @Autowired
    private AttestationAuthorizationService attestationAuthorizationService;

    private final Logger logger = LogManager.getLogger(AttestationRestController.class);

    /** 
     * Generates and returns a PDF attestation with a template depending on the targeted DSpace object type
     * 
     * @param uuid The uuid of the target DSpace Item from the request
     */
    @RequestMapping(method = RequestMethod.GET)
    public void attestation(
        HttpServletResponse response,
        HttpServletRequest request,
        @PathVariable UUID uuid
    ) throws IOException {
        Context context = ContextUtil.obtainContext(request);
        try {
            if (this.checkAuthorization(context, uuid)) {
                // If the authorization check passes, handler cannot be null.
                // See why in 'AttestationAuthorizationService.isItemValidForAttestation'
                PDFAttestationGeneratorHandler handler = PDFAttestationGeneratorFactory
                        .getInstance()
                        .getHandlerInstance(uuid);
                try {
                    response.setContentType("application/pdf");
                    handler.getAttestation(response.getOutputStream(), uuid);
                    response.flushBuffer();
                } catch (PDFGenerationException e) {
                    response.sendError(500, "An error occurred while generating the attestation");
                }
            } else {
                response.sendError(401, "You are not authorize to access this resource");
            }
        } catch (SQLException e) {
            response.sendError(404, "Object not found");
        } catch (HandlerNotFoundException e) {
            response.sendError(422, "No handler configured for this type of item");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Endpoint to trigger the sending of both attestation emails for a specific item.
     * Try to send emails to authors and supervisors.
     * 
     * @param response The current http response object.
     * @param request The current http request object.
     * @param uuid The uuid of the item to send the emails for.
     */
    @RequestMapping(method = RequestMethod.POST, value = "/sendEmailAttestations")
    public ResponseEntity<?> sendAttestationEmails(
        HttpServletResponse response,
        HttpServletRequest request,
        @PathVariable UUID uuid
    ) {
        Context context = ContextUtil.obtainContext(request);
        try {
            if (!this.checkAuthorization(context, uuid)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // No need to check if item is null because it is already done by checkAuthorization()
            Item item = itemService.find(context, uuid);

            // Try to send both attestation emails
            new ThesisAuthorAttestationEmail(context, item).sendEmail();
            new ThesisSupervisorAttestationEmail(context, item).sendEmail();

            return ResponseEntity.ok("Attestation emails sent");
        } catch (Exception e) {
            logger.error("Could not send attestation email :: " + e.getMessage(), e);
            return ResponseEntity.internalServerError().body("An error occurred sending attestation emails");
        }
    }

    private Boolean checkAuthorization(Context context, UUID uuid) throws SQLException {
        Item dsItem = itemService.find(context, uuid);
        if (dsItem == null) {
            return false;
        }
        return attestationAuthorizationService.isItemValidForAttestation(dsItem, context)
            && attestationAuthorizationService.isUserAuthorized(dsItem, context);
    }
}
