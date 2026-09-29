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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.listener.MetadataListener;
import org.dspace.submit.listener.SimpleMetadataListener;
import org.dspace.uclouvain.content.enrichment.ItemEnrichment;
import org.dspace.uclouvain.content.enrichment.ItemEnrichment.Status;
import org.dspace.uclouvain.external.ExternalSourceClient;
import org.dspace.uclouvain.external.ExternalSourceException;
import org.dspace.uclouvain.external.importer.json.crossref.UCLouvainCrossRefImportSourceService;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.dspace.uclouvain.itemEnhancer.poller.UCLouvainItemEnhancerPoller;
import org.dspace.uclouvain.services.ItemEnrichmentService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The {@link EnrichMetadataConsumer} enriches a publication from its external identifier, and records every
 * attempt so that a source is queried once per identifier value:
 * <ul>
 *   <li>a SUCCESS or NOT_FOUND attempt for the current identifier value: the source is not called again,</li>
 *   <li>a changed identifier value: the source is called again,</li>
 *   <li>an ERROR attempt: the source is called again once the retry delay has elapsed.</li>
 * </ul>
 * The consumer runs inside the {@code context.commit()} of the caller, which is the enhancer poller thread when a
 * Person is renamed: the Context it receives is the only one it may use there (see the 2026-09 production
 * incident), and the CrossRef extraction needs it for its Solr lookups.
 */
public class EnrichMetadataConsumerIT extends AbstractIntegrationTestWithDatabase {

    private static final String RETRY_DELAY_PROPERTY = "uclouvain.enrichment.retry-delay";
    private static final String PROVIDER = "crossref";
    private static final String DOI = "10.1000/probe";
    private static final String OTHER_DOI = "10.1000/corrected";
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
    private final ItemEnrichmentService enrichmentService =
        UCLouvainServiceFactory.getInstance().getItemEnrichmentService();

    private UCLouvainCrossRefImportSourceService crossRefService;
    private ExternalSourceClient originalClient;
    private ExternalSourceClient client;
    private String originalRetryDelay;
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
        // Until it is armed with an answer it returns null, i.e. "not found".
        crossRefService = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("customCrossRefImportService", UCLouvainCrossRefImportSourceService.class);
        originalClient = (ExternalSourceClient) ReflectionTestUtils.getField(crossRefService, "externalSourceClient");
        client = mock(ExternalSourceClient.class);
        ReflectionTestUtils.setField(crossRefService, "externalSourceClient", client);
        originalRetryDelay = configurationService.getProperty(RETRY_DELAY_PROPERTY);

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Global").build();
        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        ReflectionTestUtils.setField(crossRefService, "externalSourceClient", originalClient);
        configurationService.setProperty(RETRY_DELAY_PROPERTY, originalRetryDelay);
        super.destroy();
        enhancerService.cleanForDateRange(context, new Date(0), new Date());
    }

    /**
     * #1. Create a Person and a Publication (with a DOI) whose author is linked to the Person, while CrossRef is
     *     down: the attempt is recorded as an error.
     * #2. CrossRef is back. Give the Person a new pen name and run the enhancer poller.
     * #3. The publication carries the new name, and it was enriched from CrossRef by the consumer running in
     *     the poller thread.
     */
    @Test
    public void renamingAPersonUpdatesAndEnrichesItsPublications() throws Exception {
        setRetryDelay(0);
        doThrow(new ExternalSourceException("GET crossref answered HTTP 500 Internal Server Error"))
            .when(client).get(endsWith(DOI));
        context.turnOffAuthorisationSystem();
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
        assertEquals(Status.ERROR, latestAttempt(publication).getStatus());

        // The CREATE events queued the items for enhancement: drop them so that only the rename is processed.
        enhancerService.cleanForDateRange(context, new Date(0), new Date());
        assertThat(enhancerService.countItemsToEnhance(context), equalTo(0));
        // CrossRef answers again, and only the poller run below can reach it.
        clearInvocations(client);
        doReturn(CROSSREF_ANSWER).when(client).get(endsWith(DOI));

        person = context.reloadEntity(person);
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(context, person, "dc", "title", null, null, PEN_NAME);
        itemService.update(context, person);
        context.restoreAuthSystemState();
        context.commit();
        assertThat(enhancerService.countItemsToEnhance(context), equalTo(1));

        poller.run();

        // The consumer did run in the poller thread and reached CrossRef for the publication.
        verify(client).get(endsWith(DOI));
        publication = context.reloadEntity(publication);
        List<MetadataValue> metadata = publication.getMetadata();
        // 1) The pen name was propagated by the enhancer (poller)
        assertThat(metadata, hasItem(with("dc.contributor.author", PEN_NAME, personAuthority, CF_ACCEPTED)));
        // 2) The publication was enriched from the CrossRef answer...
        assertThat(metadata, hasItem(with("dc.description.abstract", "Probe abstract")));
        // 3) ...journal included. NOTE: the local Journal is not resolved here (no authority) because the upstream
        //    test-discovery.xml overrides the fork's ISSN search filter, so 'issn_keyword' is never indexed in ITs.
        assertThat(metadata, hasItem(with("dc.relation.journal", "Probe journal, as spelled by CrossRef")));
        // 4) The attempt is recorded.
        assertEquals(Status.SUCCESS, latestAttempt(publication).getStatus());
        // 5) The metadata added by the consumer re-queued the publication for enhancement: the next poller cycle
        //    is a no-op, the queue converges and CrossRef is not called again.
        poller.run();
        assertThat(enhancerService.countItemsToEnhance(context), equalTo(0));
        verify(client).get(endsWith(DOI));
    }

    @Test
    public void anEnrichedPublicationIsNotEnrichedAgain() throws Exception {
        doReturn(CROSSREF_ANSWER).when(client).get(endsWith(DOI));
        Item publication = createPublication(DOI);
        assertEquals(Status.SUCCESS, latestAttempt(publication).getStatus());
        clearInvocations(client);

        modify(publication);

        verify(client, never()).get(anyString());
    }

    @Test
    public void aCorrectedDoiIsEnrichedAgain() throws Exception {
        doReturn(CROSSREF_ANSWER).when(client).get(anyString());
        Item publication = createPublication(DOI);
        clearInvocations(client);

        publication = context.reloadEntity(publication);
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(context, publication, "dc", "identifier", "doi", null, OTHER_DOI);
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        context.commit();

        verify(client).get(endsWith(OTHER_DOI));
        ItemEnrichment latest = enrichmentService.findLatest(context, publication, PROVIDER, OTHER_DOI);
        assertNotNull(latest);
        assertEquals(Status.SUCCESS, latest.getStatus());
    }

    @Test
    public void anUnknownDoiIsRecordedAndNotRetried() throws Exception {
        doReturn(null).when(client).get(endsWith(DOI));
        Item publication = createPublication(DOI);
        assertEquals(Status.NOT_FOUND, latestAttempt(publication).getStatus());
        clearInvocations(client);

        modify(publication);

        verify(client, never()).get(anyString());
    }

    @Test
    public void aSourceFailureIsRetriedAfterTheDelay() throws Exception {
        setRetryDelay(3600);
        doThrow(new ExternalSourceException("GET crossref answered HTTP 500 Internal Server Error"))
            .when(client).get(endsWith(DOI));
        Item publication = createPublication(DOI);
        ItemEnrichment latest = latestAttempt(publication);
        assertEquals(Status.ERROR, latest.getStatus());
        assertThat(latest.getReason(), containsString("500"));
        clearInvocations(client);

        // Within the delay: no new attempt.
        modify(publication);
        verify(client, never()).get(anyString());

        // Once the delay has elapsed: a new attempt.
        setRetryDelay(0);
        modify(publication);
        verify(client).get(endsWith(DOI));
    }

    /**
     * The upstream providers wrap their failures in a plain RuntimeException: it is recorded like any other failure
     * and the remaining providers of the item still run.
     */
    @Test
    public void aFailingProviderIsRecordedAndDoesNotStopTheOthers() throws Exception {
        SimpleMetadataListener listener = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName(MetadataListener.class.getName(), SimpleMetadataListener.class);
        ExternalDataProvider failing = mock(ExternalDataProvider.class);
        when(failing.getSourceIdentifier()).thenReturn("pubmed");
        when(failing.getExternalDataObject(any(), anyString())).thenThrow(new RuntimeException("pubmed is down"));
        // 'dc.identifier.pmid' comes before 'dc.identifier.doi' in the test listener configuration.
        List<ExternalDataProvider> original = listener.getExternalDataProvidersMap()
            .put("dc.identifier.pmid", List.of(failing));
        try {
            doReturn(CROSSREF_ANSWER).when(client).get(endsWith(DOI));
            context.turnOffAuthorisationSystem();
            Item publication = ItemBuilder.createItem(context, collection)
                .withEntityType("Publication")
                .withTitle("Probe publication")
                .withMetadata("dc", "identifier", "pmid", "12345")
                .withDoiIdentifier(DOI)
                .build();
            context.restoreAuthSystemState();
            context.commit();

            ItemEnrichment pubmed = enrichmentService.findLatest(context, publication, "pubmed", "12345");
            assertNotNull(pubmed);
            assertEquals(Status.ERROR, pubmed.getStatus());
            assertThat(pubmed.getReason(), containsString("pubmed is down"));
            assertEquals(Status.SUCCESS, latestAttempt(publication).getStatus());
            assertThat(context.reloadEntity(publication).getMetadata(),
                hasItem(with("dc.description.abstract", "Probe abstract")));
        } finally {
            listener.getExternalDataProvidersMap().put("dc.identifier.pmid", original);
        }
    }

    private Item createPublication(String doi) throws Exception {
        context.turnOffAuthorisationSystem();
        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Probe publication")
            .withDoiIdentifier(doi)
            .build();
        context.restoreAuthSystemState();
        context.commit();
        return publication;
    }

    /** Any metadata change fires the MODIFY_METADATA event the consumer listens to. */
    private void modify(Item publication) throws Exception {
        publication = context.reloadEntity(publication);
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, publication, "dc", "subject", null, null, "touched " + System.nanoTime());
        itemService.update(context, publication);
        context.restoreAuthSystemState();
        context.commit();
    }

    private ItemEnrichment latestAttempt(Item publication) throws Exception {
        ItemEnrichment latest = enrichmentService.findLatest(context, publication, PROVIDER, DOI);
        assertNotNull("no enrichment attempt recorded", latest);
        return latest;
    }

    private void setRetryDelay(int seconds) {
        configurationService.setProperty(RETRY_DELAY_PROPERTY, String.valueOf(seconds));
    }
}
