/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.guards;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import org.dspace.AbstractDSpaceTest;
import org.dspace.content.WorkspaceItem;
import org.dspace.services.ConfigurationService;
import org.dspace.services.email.EmailServiceImpl;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowStartVetoException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link MailServerAliveGuard}.
 * The exemption rule is switched off here: evaluating it requires a database, so it is covered by
 * the integration test instead.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class MailServerAliveGuardTest extends AbstractDSpaceTest {

    private static final int TIMEOUT = 500;

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();

    /** Never touched by the guard: no exemption rule is configured, so the item is not read. */
    private final WorkspaceItem workspaceItem = mock(WorkspaceItem.class);

    private ServerSocket fakeMailServer;

    @Before
    public void setUp() {
        configService.setProperty("uclouvain.mail_server_check.exemption_rule", null);
        configService.setProperty("uclouvain.mail_server_check.timeout", TIMEOUT);
        configService.setProperty("mail.server", "127.0.0.1");
    }

    @After
    public void tearDown() throws IOException {
        if (this.fakeMailServer != null) {
            this.fakeMailServer.close();
        }
        configService.reloadConfig();
        mailSession().reset();
    }

    @Test
    public void noMailIsSentSoNothingIsChecked() throws IOException {
        useMailServerPort(closedPort());
        configService.setProperty("mail.server.disabled", true);
        configService.setProperty("mail.server.fixedRecipient", null);

        // No veto: the instance doesn't send any mail, a dead mail server is not a problem.
        guard().check(null, this.workspaceItem);
    }

    @Test
    public void disabledMailServerWithAFixedRecipientIsStillChecked() throws IOException {
        useMailServerPort(closedPort());
        // `mail.server.disabled` alone doesn't mean "no mail": with a fixed recipient, `Email`
        // still sends everything to that address. This is how the dev instances are configured.
        configService.setProperty("mail.server.disabled", true);
        configService.setProperty("mail.server.fixedRecipient", "someone@example.com");

        assertThrows(WorkflowStartVetoException.class, () -> guard().check(null, this.workspaceItem));
    }

    @Test
    public void aliveMailServerLetsTheDepositThrough() throws IOException {
        useMailServerPort(politeMailServerPort());
        configService.setProperty("mail.server.disabled", false);

        guard().check(null, this.workspaceItem);
    }

    @Test
    public void deadMailServerVetoesTheDeposit() throws IOException {
        useMailServerPort(closedPort());
        configService.setProperty("mail.server.disabled", false);

        assertThrows(WorkflowStartVetoException.class, () -> guard().check(null, this.workspaceItem));
    }

    /**
     * A mail server that accepts the connection and then stays silent is the case the timeouts are
     * there for: without them `Transport#connect` never returns. The JUnit timeout is part of the
     * assertion - it turns a hang into a failure.
     */
    @Test(timeout = 10 * TIMEOUT)
    public void muteMailServerVetoesTheDepositWithoutHanging() throws IOException {
        useMailServerPort(muteMailServerPort());
        configService.setProperty("mail.server.disabled", false);

        assertThrows(WorkflowStartVetoException.class, () -> guard().check(null, this.workspaceItem));
    }

    // PRIVATE METHODS =================================================================================================

    /** Build the guard. It reads its configuration once, so it must be built last. */
    private MailServerAliveGuard guard() {
        return new MailServerAliveGuard();
    }

    private EmailServiceImpl mailSession() {
        return (EmailServiceImpl) DSpaceServicesFactory.getInstance().getEmailService();
    }

    /** Point the mail session to the given port; the session caches it, hence the reset. */
    private void useMailServerPort(int port) {
        configService.setProperty("mail.server.port", port);
        mailSession().reset();
    }

    /** A port nobody listens on: the connection is refused immediately. */
    private int closedPort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /** A mail server answering the greeting, the EHLO and the QUIT. */
    private int politeMailServerPort() throws IOException {
        this.fakeMailServer = new ServerSocket(0);
        new Thread(() -> {
            try (Socket socket = this.fakeMailServer.accept()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.print("220 fake ESMTP ready\r\n");
                out.flush();
                String line;
                while ((line = in.readLine()) != null) {
                    out.print(line.toUpperCase().startsWith("QUIT") ? "221 bye\r\n" : "250 ok\r\n");
                    out.flush();
                }
            } catch (IOException ignored) {
                // the socket is closed by the test teardown
            }
        }).start();
        return this.fakeMailServer.getLocalPort();
    }

    /** A mail server accepting the connection and then never saying anything. */
    private int muteMailServerPort() throws IOException {
        this.fakeMailServer = new ServerSocket(0);
        new Thread(() -> {
            try (Socket socket = this.fakeMailServer.accept()) {
                Thread.sleep(60000);
            } catch (IOException | InterruptedException ignored) {
                // the socket is closed by the test teardown
            }
        }).start();
        return this.fakeMailServer.getLocalPort();
    }
}
