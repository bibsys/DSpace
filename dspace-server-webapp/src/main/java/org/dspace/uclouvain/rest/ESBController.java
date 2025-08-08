/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest;

import org.apache.http.HttpStatus;
import org.dspace.uclouvain.core.model.PersonEventModel;
import org.dspace.uclouvain.rabbitMQ.connectors.PersonEventConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
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
    @RequestMapping(method = RequestMethod.POST, value = "/person/event")
    public ResponseEntity<?> postPersonEvent(@RequestBody PersonEventModel event) {
        String action = event.getAction();
        // Check that action is valid before processing event.
        if (!PersonEventModel.AVAILABLE_ACTIONS.contains(action)) {
            logger.warn("Wrong action type detected on ESB event: " + action);
            return ResponseEntity.status(HttpStatus.SC_BAD_REQUEST).body(
                "Only " + String.join(", ", PersonEventModel.AVAILABLE_ACTIONS) + " action types are valid."
            );
        }
        try {
            // Publish the message into rabbit.
            connector.publishJSONMessage(event);
            return ResponseEntity.status(HttpStatus.SC_CREATED).body("Event handled correctly.");
        } catch (Exception e) {
            logger.error("Cloud not process an event sent from ESB: "
                + "Action: " + event.getAction() + " FSG: " + event.getFgs()
                + " Information: " + event.getInformation(), e
            );
            return ResponseEntity.internalServerError().body("Error while processing event.");
        }
    }
}
