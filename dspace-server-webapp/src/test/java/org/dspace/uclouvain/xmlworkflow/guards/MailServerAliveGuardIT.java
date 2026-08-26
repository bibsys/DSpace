/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.guards;

import static com.jayway.jsonpath.JsonPath.read;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicReference;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.WorkflowItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.email.EmailServiceImpl;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link MailServerAliveGuard}: the deposit endpoint must refuse to start a
 * workflow while the mail server is down, and must keep accepting the deposits that send no mail.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class MailServerAliveGuardIT extends AbstractControllerIntegrationTest {

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @After
    public void resetMailConfiguration() {
        configurationService.reloadConfig();
        mailService().reset();
    }

    @Test
    public void depositIsRefusedWhileTheMailServerIsDown() throws Exception {
        WorkspaceItem workspaceItem = givenAWorkspaceItem(false);
        givenADeadMailServer();

        String token = getAuthToken(workspaceItem.getSubmitter().getEmail(), password);
        getClient(token)
            .perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                .content("/api/submission/workspaceitems/" + workspaceItem.getID())
                .contentType(textUriContentType))
            .andExpect(status().isUnprocessableEntity())
            // under MockMvc a `sendError` has no rendered body: the message is the error reason
            .andExpect(status().reason(containsString("mail.server.unavailable.exception")));

        // Nothing was created and the submission is left exactly as it was.
        getClient(token).perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()))
            .andExpect(status().isOk());
        getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/workflow/workflowitems"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void exemptedDepositGoesThroughWhileTheMailServerIsDown() throws Exception {
        WorkspaceItem workspaceItem = givenAWorkspaceItem(true);
        givenADeadMailServer();

        // the workflow item is created by the endpoint, so no builder will clean it up for us
        AtomicReference<Integer> createdId = new AtomicReference<>();
        String token = getAuthToken(workspaceItem.getSubmitter().getEmail(), password);
        try {
            getClient(token)
                .perform(post(BASE_REST_SERVER_URL + "/api/workflow/workflowitems")
                    .content("/api/submission/workspaceitems/" + workspaceItem.getID())
                    .contentType(textUriContentType))
                .andExpect(status().isCreated())
                .andDo(result -> createdId.set(read(result.getResponse().getContentAsString(), "$.id")));
        } finally {
            if (createdId.get() != null) {
                WorkflowItemBuilder.deleteWorkflowItem(createdId.get());
            }
        }
    }

    // PRIVATE METHODS =================================================================================================

    /**
     * Build a submission ready to be deposited.
     *
     * @param exempted whether the item carries the metadata exempting it from the check
     * @return the workspace item to deposit
     */
    private WorkspaceItem givenAWorkspaceItem(boolean exempted) throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Collection")
            .withWorkflowGroup(1, admin)
            .build();
        EPerson submitter = EPersonBuilder.createEPerson(context)
            .withEmail("submitter@example.com")
            .withPassword(password)
            .build();
        context.setCurrentUser(submitter);

        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("Submission item")
            .withIssueDate("2026-08-26")
            .withSubmitter(submitter)
            .withFulltext("thesis.txt", "/local/path/thesis.txt", "thesis content".getBytes(UTF_8))
            .grantLicense()
            .build();
        // a file and the access condition acknowledgement are what makes a submission valid here,
        // without them the endpoint answers 422 for a validation error, never reaching the guards
        itemService.addMetadata(
            context, workspaceItem.getItem(), "dspace", "file-access-condition", "acknowledgement", null, "true");
        if (exempted) {
            itemService.addMetadata(
                context, workspaceItem.getItem(), "dcterms", "provenance", null, null, "cataretro");
            itemService.update(context, workspaceItem.getItem());
        }
        context.restoreAuthSystemState();
        // the deposit runs in another context: commit, then take a fresh handle on the submission
        context.commit();
        return workspaceItemService.find(context, workspaceItem.getID());
    }

    /**
     * Make the guard bite: a fixed recipient means mails are really sent even though the mail
     * server is flagged as disabled, and the mail session is pointed to a port nobody listens on.
     */
    private void givenADeadMailServer() throws IOException {
        configurationService.setProperty("mail.server.fixedRecipient", "someone@example.com");
        configurationService.setProperty("mail.server", "127.0.0.1");
        try (ServerSocket socket = new ServerSocket(0)) {
            configurationService.setProperty("mail.server.port", socket.getLocalPort());
        }
        mailService().reset();
    }

    private EmailServiceImpl mailService() {
        return (EmailServiceImpl) DSpaceServicesFactory.getInstance().getEmailService();
    }
}
