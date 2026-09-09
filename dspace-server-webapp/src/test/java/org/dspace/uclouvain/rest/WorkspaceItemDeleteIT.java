/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.eperson.EPerson;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Deletion of a Publication workspace item through the REST API.
 * The item deletion flow requires DELETE and REMOVE on the item and on every bundle, and DELETE and WRITE on
 * every bitstream; a non-submitter holds none of those policies, so the UCLouvain authorize layer must grant them.
 */
public class WorkspaceItemDeleteIT extends AbstractControllerIntegrationTest {

    private EPerson submitter;
    private EPerson author;
    private EPerson stranger;
    private WorkspaceItem workspaceItem;

    @Before
    public void setup() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection publications = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .withName("Publications")
            .build();
        Collection persons = CollectionBuilder.createCollection(context, parentCommunity)
            .withEntityType("Person")
            .withName("Persons")
            .build();

        submitter = createEPerson("submitter@example.com");
        author = createEPerson("author@example.com");
        stranger = createEPerson("stranger@example.com");
        Item authorProfile = ItemBuilder.createItem(context, persons)
            .withTitle("Author, An")
            .withDspaceObjectOwner(author)
            .build();

        workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, publications)
            .withSubmitter(submitter)
            .withEntityType("Publication")
            .withTitle("A publication with a file")
            .withAuthor(authorProfile.getName(), authorProfile.getID().toString())
            .withFulltext("fulltext.txt", "/local/fulltext.txt", "some content".getBytes(UTF_8))
            .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void authorCanDeleteWorkspaceItemWithFiles() throws Exception {
        deleteAs(author).andExpect(status().isNoContent());
        findAsAdmin().andExpect(status().isNotFound());
    }

    @Test
    public void submitterCanDeleteWorkspaceItemWithFiles() throws Exception {
        deleteAs(submitter).andExpect(status().isNoContent());
        findAsAdmin().andExpect(status().isNotFound());
    }

    @Test
    public void strangerCannotDeleteWorkspaceItem() throws Exception {
        deleteAs(stranger).andExpect(status().isForbidden());
        findAsAdmin().andExpect(status().isOk());
    }

    private EPerson createEPerson(String email) {
        return EPersonBuilder.createEPerson(context).withEmail(email).withPassword(password).build();
    }

    private ResultActions deleteAs(EPerson user) throws Exception {
        return getClient(getAuthToken(user.getEmail(), password))
            .perform(delete("/api/submission/workspaceitems/" + workspaceItem.getID()));
    }

    private ResultActions findAsAdmin() throws Exception {
        return getClient(getAuthToken(admin.getEmail(), password))
            .perform(get("/api/submission/workspaceitems/" + workspaceItem.getID()));
    }
}
