/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rabbitMQ.connectors;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.factories.RabbitMQConnectionFactory;

/**
 * Abstract rabbitMQ connector to perform basic action on a queue.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public abstract class RabbitMQGenericConnector {
    protected Connection rabbitClient = getRabbitMQConnection();
    protected ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Publish a basic JSON message to RabbitMQ queue.
     * @param message An object that will be converted to a JSON string.
     * @throws IOException
     */
    public void publishJSONMessage(Object message) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonEncoded = objectMapper.writeValueAsString(message);
        // Get a channel for RabbitMQ and publish the message into queue.
        publishMessage(getChannel(), jsonEncoded);
    }

    /**
     * Retrieve a channel for a rabbitMQ connection.
     */
    public abstract Channel getChannel() throws Exception;
    /**
     * Publish a message using the provided channel.
     * @param channel The channel to use to publish the message.
     * @param message The message string to publish to RabbitMQ.
     */
    protected abstract void publishMessage(Channel channel, String message) throws Exception;

    /**
     * Retrieve a connection to RabbitMQ server.
     *
     * @return A connection to RabbitMQ.
     */
    static protected Connection getRabbitMQConnection() {
        try {
            return RabbitMQConnectionFactory.getInstance().newConnection();
        } catch (Exception e) {
            throw new RuntimeException("Could not retrieve RabbitMQ client connection.", e);
        }
    }
}
