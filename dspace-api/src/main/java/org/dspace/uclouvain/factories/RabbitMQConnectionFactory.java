/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.factories;

import com.rabbitmq.client.ConnectionFactory;
import org.dspace.utils.DSpace;

/**
 * Factory to return a instance of the rabbitMQFactory.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class RabbitMQConnectionFactory {
    protected RabbitMQConnectionFactory() {
        throw new UnsupportedOperationException();
    }
    public static ConnectionFactory getInstance() {
        return (ConnectionFactory) (
            new DSpace()
                .getServiceManager()
                .getApplicationContext()
                .getBean("rabbitConnectionFactory")
            );
    }
}
