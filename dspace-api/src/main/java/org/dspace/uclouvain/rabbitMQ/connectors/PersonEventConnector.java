/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rabbitMQ.connectors;

import java.io.IOException;

import com.rabbitmq.client.Channel;

/**
 * Specific RabbitMQ connector to work with person event queue.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class PersonEventConnector extends RabbitMQGenericConnector {
    protected String queueName = configService.getProperty("uclouvain.person_event.rabbit.queue");

    protected Channel declareChannel() throws IOException {
        Channel channel = rabbitClient.createChannel();
        channel.queueDeclare(queueName, true, false, false, null);
        channel.basicQos(1);
        return channel;
    }

    protected void publishMessage(Channel channel, String message) throws IOException {
        channel.basicPublish("", queueName, null, message.getBytes());
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
}
