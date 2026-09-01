/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest.configuration;

import org.dspace.services.ConfigurationService;
import org.dspace.services.EmailService;
import org.dspace.uclouvain.rest.health.MailHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registration of the UCLouvain custom actuator health indicators, mirroring the upstream
 * {@code org.dspace.app.rest.configuration.ActuatorConfiguration}.
 */
@Configuration
public class UCLouvainActuatorConfiguration {

    /**
     * Health indicator reporting the outgoing mail server status as the {@code mail} component
     * of the {@code /actuator/health} endpoint. Can be turned off with
     * {@code management.health.mail.enabled = false}.
     *
     * @param emailService  the service providing the mail session used for real sends
     * @param configService the DSpace configuration service
     * @return the mail health indicator
     */
    @Bean
    @ConditionalOnEnabledHealthIndicator("mail")
    @ConditionalOnProperty("mail.server")
    public MailHealthIndicator mailHealthIndicator(EmailService emailService, ConfigurationService configService) {
        return new MailHealthIndicator(emailService, configService);
    }

}
