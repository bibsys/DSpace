/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest.health;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.EmailService;
import org.eclipse.angus.mail.smtp.SMTPTransport;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * Implementation of {@link HealthIndicator} that reports the status of the outgoing mail server.
 *
 * <p>The probe checks, without sending any message:
 * <ul>
 *   <li><b>connection</b>: the SMTP server is reachable (TCP + EHLO + STARTTLS if configured);</li>
 *   <li><b>authentication</b>: the configured credentials ({@code mail.server.username} /
 *       {@code mail.server.password}) are accepted;</li>
 *   <li><b>acceptMessage</b>: the server accepts an envelope from {@code mail.from.address}
 *       ({@code MAIL FROM} immediately followed by {@code RSET}, so nothing is ever queued).</li>
 * </ul>
 *
 * <p>When {@code mail.server.disabled = true} the indicator reports {@code UNKNOWN} and no probe
 * is performed at all.
 *
 * <p>The probe uses the same session configuration as real sends (from {@link EmailService}), only
 * adding socket timeouts so that a dead server cannot hang the health endpoint. Since the health
 * endpoint is polled (anonymously by the frontend, and by monitoring tools), the probe result is
 * cached for {@code uclouvain.health.mail.cache-ttl} seconds to avoid hammering the SMTP relay.
 */
public class MailHealthIndicator extends AbstractHealthIndicator {

    private static final String CACHE_TTL_PROPERTY = "uclouvain.health.mail.cache-ttl";
    private static final String TIMEOUT_PROPERTY = "uclouvain.health.mail.timeout";

    private final EmailService emailService;
    private final ConfigurationService configurationService;

    private Health cachedHealth;
    private long cachedAt;

    /**
     * Constructor.
     *
     * @param emailService         the service providing the mail session used for real sends
     * @param configurationService the DSpace configuration service
     */
    public MailHealthIndicator(EmailService emailService, ConfigurationService configurationService) {
        this.emailService = emailService;
        this.configurationService = configurationService;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        if (configurationService.getBooleanProperty("mail.server.disabled", false)) {
            builder.unknown().withDetail("reason", "Email sending is disabled (mail.server.disabled = true)");
            return;
        }
        Health health = probeWithCache();
        builder.status(health.getStatus()).withDetails(health.getDetails());
    }

    /**
     * Returns the last probe result if it is fresh enough, otherwise runs a new probe.
     * Synchronized so concurrent health requests trigger at most one SMTP probe.
     */
    private synchronized Health probeWithCache() {
        long ttlMillis = configurationService.getLongProperty(CACHE_TTL_PROPERTY, 60) * 1000;
        long now = System.currentTimeMillis();
        if (cachedHealth == null || now - cachedAt >= ttlMillis) {
            cachedHealth = probeMailServer();
            cachedAt = now;
        }
        return cachedHealth;
    }

    private Health probeMailServer() {
        Health.Builder health = new Health.Builder();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("server", configurationService.getProperty("mail.server", "not configured"));
        details.put("port", configurationService.getProperty("mail.server.port", "25"));

        String username = configurationService.getProperty("mail.server.username");
        try (Transport transport = createTransport()) {
            try {
                if (StringUtils.isBlank(username)) {
                    transport.connect();
                } else {
                    transport.connect(username, configurationService.getProperty("mail.server.password"));
                }
            } catch (AuthenticationFailedException e) {
                details.put("connection", "OK");
                details.put("authentication", "FAILED: " + e.getMessage());
                return health.down().withDetails(details).build();
            } catch (MessagingException e) {
                details.put("connection", "FAILED: " + e.getMessage());
                return health.down().withDetails(details).build();
            }
            details.put("connection", "OK");
            details.put("authentication", StringUtils.isBlank(username)
                ? "SKIPPED (no mail.server.username configured)"
                : "OK"
            );

            String from = configurationService.getProperty("mail.from.address");
            if (StringUtils.isBlank(from)) {
                details.put("acceptMessage", "SKIPPED (no mail.from.address configured)");
            } else if (transport instanceof SMTPTransport smtpTransport) {
                try {
                    smtpTransport.issueCommand("MAIL FROM:<" + from + ">", 250);
                    smtpTransport.issueCommand("RSET", 250);
                    details.put("acceptMessage", "OK (envelope sender <" + from + "> accepted)");
                } catch (MessagingException e) {
                    details.put("acceptMessage", "FAILED: " + e.getMessage());
                    return health.down().withDetails(details).build();
                }
            } else {
                details.put("acceptMessage", "SKIPPED (unsupported transport " + transport.getClass().getName() + ")");
            }
            return health.up().withDetails(details).build();
        } catch (MessagingException e) {
            return health.down().withDetails(details).withDetail("reason", e.getMessage()).build();
        }
    }

    /**
     * Creates the transport used to probe the mail server: the session properties used for real
     * sends, plus short socket timeouts. Protected so tests can substitute a mock transport.
     *
     * @return an unconnected SMTP transport
     * @throws MessagingException if no transport provider is available
     */
    protected Transport createTransport() throws MessagingException {
        Session emailSession = emailService.getSession();
        Properties properties = new Properties();
        if (emailSession != null) {
            properties.putAll(emailSession.getProperties());
        }
        String timeout = configurationService.getProperty(TIMEOUT_PROPERTY, "10000");
        String protocol = properties.getProperty("mail.transport.protocol", "smtp");
        properties.putIfAbsent("mail." + protocol + ".connectiontimeout", timeout);
        properties.putIfAbsent("mail." + protocol + ".timeout", timeout);
        properties.putIfAbsent("mail." + protocol + ".writetimeout", timeout);
        return Session.getInstance(properties).getTransport();
    }

}
