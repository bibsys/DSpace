/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.services.RequestService;
import org.dspace.uclouvain.external.importer.json.crossref.UCLouvainCrossRefImportSourceService;
import org.dspace.utils.DSpace;
import org.dspace.web.ContextUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The import source services run inside REST requests (external source lookup, workspace item creation from an
 * external entry, metadata extraction step). Hibernate binds its session to the thread, so a second Context opened
 * and closed by the service would roll back and close the session of the request Context itself: every entity the
 * request had loaded becomes detached ("detached entity passed to persist") and the transaction is left
 * MARKED_ROLLBACK. The services must therefore work with the request Context and never close it.
 */
public class ImportSourceRequestContextIT extends AbstractIntegrationTestWithDatabase {

    private static final String CROSSREF_ANSWER = """
        {"message": {"type": "report", "title": ["Probe title"], "language": "en",
                     "issued": {"date-parts": [[2020, 1, 1]]}, "subject": ["probe"]}}
        """;

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final RequestService requestService = new DSpace().getRequestService();
    private final UCLouvainCrossRefImportSourceService crossRefService = new UCLouvainCrossRefImportSourceService();
    private boolean requestStarted = false;

    @Before
    public void stubHttpClient() {
        LiveImportClient client = mock(LiveImportClient.class);
        when(client.executeHttpGetRequest(anyInt(), anyString(), any())).thenReturn(CROSSREF_ANSWER);
        ReflectionTestUtils.setField(crossRefService, "liveImportClient", client);
    }

    @After
    public void endRequest() {
        if (requestStarted) {
            requestService.endRequest(null);
        }
    }

    @Test
    public void importKeepsTheRequestContextUsable() throws Exception {
        context.turnOffAuthorisationSystem();
        Collection collection = CollectionBuilder
            .createCollection(context, CommunityBuilder.createCommunity(context).build()).build();
        UUID itemId = ItemBuilder.createItem(context, collection).withTitle("probe").build().getID();
        context.commit();
        // Simulate a REST request whose Context is the test one, exactly as DSpace stores it in the servlet request
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ContextUtil.DSPACE_CONTEXT)).thenReturn(context);
        when(request.getLocale()).thenReturn(Locale.ENGLISH);
        requestService.startRequest(request, mock(HttpServletResponse.class));
        requestStarted = true;
        assertSame(context, ContextUtil.obtainCurrentRequestContext());
        Item item = itemService.find(context, itemId);

        List<MetadataValueDTO> metadata = crossRefService.getMetadataList("10.1000/probe");

        // the extraction really ran (a failure inside the service is swallowed and returns an empty list)
        assertFalse("no metadata extracted, the service did not run through", metadata.isEmpty());
        // and the request Context is untouched: its entities are still attached, its transaction still active
        itemService.addMetadata(context, item, "dc", "description", null, null, "still attached");
        assertEquals("still attached", itemService.getMetadataFirstValue(item, "dc", "description", null, null));
        assertNotNull(itemService.find(context, itemId));
    }
}
