/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.guards;

import java.sql.SQLException;
import java.util.Properties;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.event.behavior.MetadataActivationRule;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowStartGuard;
import org.dspace.workflow.WorkflowStartVetoException;

/**
 * Refuse a deposit when the mail server is not able to take the mails that the deposit is about to
 * send. Depositing a master thesis triggers an attestation mail to the author and to the
 * supervisor; when the mail server is down that mail is lost silently while the item goes on
 * through the workflow as if nothing had happened.
 *
 * Two kinds of deposit are not guarded:
 *   * those made while the instance doesn't send any mail at all;
 *   * those matching `uclouvain.mail_server_check.exemption_rule`, retro-cataloged these being
 *     deposited without any attestation.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class MailServerAliveGuard implements WorkflowStartGuard {

    private static final Logger logger = LogManager.getLogger(MailServerAliveGuard.class);

    // CLASS ATTRIBUTES ================================================================================================
    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final MetadataActivationRule exemptionRule;
    private final String timeout;

    // CONSTRUCTOR =====================================================================================================
    public MailServerAliveGuard() {
        String rule = configService.getProperty("uclouvain.mail_server_check.exemption_rule");
        this.exemptionRule = StringUtils.isBlank(rule)
            ? null
            : new MetadataActivationRule(rule);
        this.timeout = String.valueOf(configService.getIntProperty("uclouvain.mail_server_check.timeout", 3000));
    }

    // GUARD IMPLEMENTATION ============================================================================================

    @Override
    public boolean isGuardForNotification() {
        // This guard check if smtp server is OK to send notifications.
        return true;
    }

    @Override
    public void check(Context context, WorkspaceItem wsi) {
        if (!isMailSendingEnabled() || isExempted(context, wsi)) {
            return;
        }
        if (!isMailServerAlive()) {
            throw new WorkflowStartVetoException("mail.server.unavailable.exception");
        }
    }

    // PRIVATE METHODS =================================================================================================

    /**
     * Check if a mail would really leave this instance.
     * Beware that `mail.server.disabled` alone doesn't mean "no mail": when a fixed recipient is
     * configured, {@link org.dspace.core.Email} still sends everything to that address - which is
     * how the development and staging instances are configured.
     *
     * @return True if mails are really sent, False otherwise
     */
    private boolean isMailSendingEnabled() {
        return !configService.getBooleanProperty("mail.server.disabled", false)
            || configService.getArrayProperty("mail.server.fixedRecipient").length > 0;
    }

    /**
     * Check if this deposit is exempted from the mail server check.
     *
     * @param context The DSpace application context
     * @param wsi The workspace item about to be deposited
     * @return True if no mail is expected for this deposit, False otherwise
     */
    private boolean isExempted(Context context, WorkspaceItem wsi) {
        if (this.exemptionRule == null) {
            return false;
        }
        try {
            return this.exemptionRule.isValid(context, wsi.getItem());
        } catch (SQLException | IllegalArgumentException e) {
            logger.error("Cannot evaluate the mail server exemption rule :: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if the mail server is able to take a mail: it must accept the connection, greet us and,
     * when credentials are configured, accept them. This is what `Transport#connect` does, and it
     * is exactly what the real send will have to do again a few milliseconds later.
     *
     * @return True if the mail server answered, False otherwise
     */
    private boolean isMailServerAlive() {
        Session session = DSpaceServicesFactory.getInstance().getEmailService().getSession();
        // `EmailServiceImpl` builds the session without any timeout, so a server accepting the
        // connection and then staying silent would freeze the deposit for as long as it pleases.
        // The properties are copied and not modified: they are shared with the real send.
        Properties properties = new Properties();
        properties.putAll(session.getProperties());
        properties.putIfAbsent("mail.transport.protocol", "smtp");
        properties.putIfAbsent("mail.smtp.connectiontimeout", this.timeout);
        properties.putIfAbsent("mail.smtp.timeout", this.timeout);

        String username = configService.getProperty("mail.server.username");
        String password = configService.getProperty("mail.server.password");
        try (Transport transport = Session.getInstance(properties).getTransport()) {
            if (StringUtils.isBlank(username)) {
                transport.connect();
            } else {
                transport.connect(username, password);
            }
            return true;
        } catch (MessagingException e) {
            logger.warn("Mail server is not able to accept an email :: {}", e.getMessage(), e);
            return false;
        }
    }
}
