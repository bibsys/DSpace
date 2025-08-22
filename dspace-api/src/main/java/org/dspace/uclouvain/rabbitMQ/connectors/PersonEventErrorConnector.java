/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rabbitMQ.connectors;

import java.util.HashMap;
import java.util.Map;

/**
 * Sub-class of {@link PersonEventConnector} to push a potential error to rabbitMQ.
 */
public class PersonEventErrorConnector extends PersonEventConnector {
    public PersonEventErrorConnector(String errorQueueName) {
        this.queueName = errorQueueName;
    }

    /**
     * Publish an error message into the configured rabbit mq error queue.
     * @param event The event that caused the error.
     * @param cause The error itself.
     * @throws Exception
     */
    public void publishErrorMessage(String event, Exception cause) throws Exception {
        Map<String, String> message = new HashMap<>();
        message.put("event", event);
        message.put("error", cause.getMessage());
        this.publishJSONMessage(message);
    }
}
