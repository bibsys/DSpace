/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest;

import jakarta.ws.rs.InternalServerErrorException;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.uclouvain.core.model.PersonEventModel;
import org.dspace.uclouvain.rabbitMQ.connectors.PersonEventConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.ControllerUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Main esb endpoints for interactions with our DSpace system.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
@RestController
@RequestMapping("/api/uclouvain/esb")
public class ESBController {
    private static final Logger logger = LoggerFactory.getLogger(ESBController.class);
    private PersonEventConnector connector = new PersonEventConnector();

    /**
     * Handle the posting of a new event into DSpace RabbitMQ event queue.
     * This endpoint will be used by ESB to send us a user event.
     * After the event has been store in RabbitMQ, further more processing can be done on it.
     * 
     * @param event The request containing event information.
     * @return A ResponseEntity object signaling the success or failure of the request.
     */
    @PreAuthorize("hasAuthority('AUTHENTICATED') && @groupSecurity.isMemberOf('Person Event API')")
    @RequestMapping(method = RequestMethod.POST, value = "/person/event")
    public ResponseEntity<?> postPersonEvent(PersonEventModel event) {
        String action = event.getAction();
        // Check that action is valid before processing event.
        if (!PersonEventModel.AVAILABLE_ACTIONS.contains(action)) {
            logger.warn("Wrong action type detected on ESB event: " + action);
            throw new DSpaceBadRequestException("Invalid event action type :: " + event);
        }
        try {
            // Publish the message into rabbit.
            connector.publishJSONMessage(event);
            return ControllerUtils.toEmptyResponse(HttpStatus.CREATED);
        } catch (Exception e) {
            logger.error("Cloud not process an event sent from ESB: " + event);
            throw new InternalServerErrorException("Error processing event :: " + event);
        }
    }
}
