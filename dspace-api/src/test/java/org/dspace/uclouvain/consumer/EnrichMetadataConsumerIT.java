/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.Journal;
import org.dspace.uclouvain.external.importer.json.crossref.UCLouvainCrossRefImportSourceService;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.dspace.uclouvain.itemEnhancer.poller.UCLouvainItemEnhancerPoller;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Renaming a Person must update the linked publications (enhancer poller) and, because those publications are
 * modified, the {@link EnrichMetadataConsumer} must be able to enrich them from their external identifier.
 *
 * The consumer runs inside the {@code context.commit()} of the poller thread, where no HTTP request exists. The
 * Context it receives in {@code end(context)} is the poller's one, and it is the only Context that may be used
 * there: opening another one in that thread shares the same Hibernate session (see the 2026-09 production
 * incident) and looking one up from the "current request" yields null. Without a usable Context the CrossRef
 * extraction fails on its first Solr lookup (the journal) and returns nothing, so this test asserts the enriched
 * metadata, not just that the external source was called.
 */
public class EnrichMetadataConsumerIT extends AbstractIntegrationTestWithDatabase {

    private static final String DOI = "10.1000/probe";
    private static final String ISSN = "1234-5678";
    private static final String INITIAL_NAME = "Gloutitout, Jean";
    private static final String PEN_NAME = "Gloutitout, Jeannot";
    private static final String CROSSREF_ANSWER = """
        {"message": {
            "type": "journal-article",
            "title": ["Probe title"],
            "abstract": "Probe abstract",
            "language": "en",
            "issued": {"date-parts": [[2020, 1, 1]]},
            "container-title": ["Probe journal, as spelled by CrossRef"],
            "issn-type": [{"type": "print", "value": "1234-5678"}]
        }}
        """;

    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final EventService eventService = EventServiceFactory.getInstance().getEventService();
    private static String[] consumers;

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final UCLouvainItemEnhancerService enhancerService =
        UCLouvainServiceFactory.getInstance().getItemEnhancerService();
    private final UCLouvainItemEnhancerPoller poller =
        UCLouvainServiceFactory.getInstance().getItemEnhancerUpdatePoller();

    private UCLouvainCrossRefImportSourceService crossRefService;
    private LiveImportClient originalClient;
    private LiveImportClient client;
    private Collection collection;

    /**
     * The test configuration does not enable the UCLouvain consumers: add the one that queues the enhancements
     * and the one under test.
     */
    @BeforeClass
    public static void enableConsumers() {
        consumers = configurationService.getArrayProperty("event.dispatcher.default.consumers");
        Set<String> enabled = new HashSet<>(Arrays.asList(consumers));
        enabled.add("authoritymetadataenhancer");
        enabled.add("enrichmetadata");
        configurationService.setProperty("event.dispatcher.default.consumers", enabled.toArray());
        eventService.reloadConfiguration();
    }

    @AfterClass
    public static void restoreConsumers() {
        configurationService.setProperty("event.dispatcher.default.consumers", consumers);
        eventService.reloadConfiguration();
    }

    @Before
    public void setup() {
        // Stub the HTTP client of the Spring-managed CrossRef service used by the 'customCrossRefDataProvider'.
        // Until it is armed with an answer it returns null, which the service turns into an empty extraction.
        crossRefService = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("customCrossRefImportService", UCLouvainCrossRefImportSourceService.class);
        originalClient = (LiveImportClient) ReflectionTestUtils.getField(crossRefService, "liveImportClient");
        client = mock(LiveImportClient.class);
        ReflectionTestUtils.setField(crossRefService, "liveImportClient", client);

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Global").build();
        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        ReflectionTestUtils.setField(crossRefService, "liveImportClient", originalClient);
        super.destroy();
        enhancerService.cleanForDateRange(context, new Date(0), new Date());
    }

    /**
     * #1. Create a Journal, a Person and a Publication (with a DOI) whose author is linked to the Person.
     * #2. Give the Person a new pen name and run the enhancer poller.
     * #3. The publication carries the new name, and it was enriched from CrossRef (abstract, journal).
     */
    @Test
    public void renamingAPersonUpdatesAndEnrichesItsPublications() throws Exception {
        context.turnOffAuthorisationSystem();
        // The 'journal' discovery configuration (which indexes the ISSN) is selected by the collection entity type.
        Collection journals = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Journals")
            .withEntityType(Journal.ENTITY_TYPE)
            .build();
        Item journal = ItemBuilder.createItem(context, journals)
            .withEntityType(Journal.ENTITY_TYPE)
            .withTitle("Probe journal")
            .withMetadata("dc", "identifier", "issn", ISSN)
            .build();
        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withMetadata("dc", "title", null, INITIAL_NAME)
            .withMetadata("person", "identifier", "fgs", "000001")
            .build();
        String personAuthority = person.getID().toString();
        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Probe publication")
            .withDoiIdentifier(DOI)
            .withAuthor(INITIAL_NAME, personAuthority, CF_ACCEPTED)
            .withMetadata("authors", "identifier", "fgs", null, "000001", personAuthority, CF_ACCEPTED)
            .build();
        context.restoreAuthSystemState();
        context.commit();

        // Clean the enhancement poller table to be sure to not have any unwanted process
        //   The CREATE events queued the items for enhancement: drop them so that only the rename is processed.
        enhancerService.cleanForDateRange(context, new Date(0), new Date());
        assertThat(enhancerService.countItemsToEnhance(context), equalTo(0));
        // Mock any Crossref call with fixed response
        //   From now on CrossRef answers, and only the poller run below can reach it.
        clearInvocations(client);
        when(client.executeHttpGetRequest(anyInt(), endsWith(DOI), any())).thenReturn(CROSSREF_ANSWER);

        // Update the pen name of the person related to publication author
        //    This will trigger enhancement poller for the linked publication
        person = context.reloadEntity(person);
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(context, person, "dc", "title", null, null, PEN_NAME);
        itemService.update(context, person);
        context.restoreAuthSystemState();
        context.commit();
        assertThat(enhancerService.countItemsToEnhance(context), equalTo(1));

        poller.run();
        // The consumer did run in the poller thread and reached CrossRef for the publication.
        verify(client).executeHttpGetRequest(anyInt(), endsWith(DOI), any());

        // Reload the publication and check all metadata from Crossref response are filled into it.
        //
        publication = context.reloadEntity(publication);
        List<MetadataValue> metadata = publication.getMetadata();
        // 1) The pen name was propagated by the enhancer (poller)
        assertThat(metadata, hasItem(with("dc.contributor.author", PEN_NAME, personAuthority, CF_ACCEPTED)));
        // 2) The publication was enriched from the CrossRef answer...
        assertThat(metadata, hasItem(with("dc.description.abstract", "Probe abstract")));
        // 3) ...journal included. NOTE: the local Journal is not resolved here (no authority) because the upstream
        //    test-discovery.xml overrides the fork's ISSN search filter, so 'issn_keyword' is never indexed in ITs.
        assertThat(metadata, hasItem(with("dc.relation.journal", "Probe journal, as spelled by CrossRef")));
    }
}
