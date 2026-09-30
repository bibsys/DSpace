/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.enrichment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Item;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.content.dao.ItemEnrichmentDAO;
import org.dspace.uclouvain.content.enrichment.ItemEnrichment;
import org.dspace.uclouvain.content.enrichment.ItemEnrichment.Status;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.ItemEnrichmentService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Persistence of the enrichment attempts: the latest attempt for (item, provider, identifier) is the one that
 * matters, and the rows follow the item's lifecycle.
 */
public class ItemEnrichmentServiceIT extends AbstractIntegrationTestWithDatabase {

    private static final String DOI_FIELD = "dc.identifier.doi";
    private static final String DOI = "10.1000/probe";

    private final ItemEnrichmentService service = UCLouvainServiceFactory.getInstance().getItemEnrichmentService();
    private final ItemEnrichmentDAO dao = DSpaceServicesFactory.getInstance().getServiceManager()
        .getServiceByName(null, ItemEnrichmentDAO.class);
    private Item item;
    private String originalRetryDelay;

    @After
    public void restoreRetryDelay() {
        setRetryDelay(originalRetryDelay);
    }

    private void setRetryDelay(String seconds) {
        DSpaceServicesFactory.getInstance().getConfigurationService()
            .setProperty("uclouvain.enrichment.retry-delay", seconds);
    }

    @Before
    public void createItem() {
        originalRetryDelay = DSpaceServicesFactory.getInstance().getConfigurationService()
            .getProperty("uclouvain.enrichment.retry-delay");
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        item = ItemBuilder.createItem(context, CollectionBuilder.createCollection(context, parentCommunity).build())
            .withTitle("Probe")
            .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void recordsAndFindsAnAttempt() throws Exception {
        assertNull(service.findLatest(context, item, "crossref", DOI));

        service.record(context, item, "crossref", DOI_FIELD, DOI, Status.SUCCESS, null);

        ItemEnrichment latest = service.findLatest(context, item, "crossref", DOI);
        assertNotNull(latest);
        assertEquals(item.getID(), latest.getItemUuid());
        assertEquals("crossref", latest.getProvider());
        assertEquals(DOI_FIELD, latest.getIdentifierField());
        assertEquals(DOI, latest.getIdentifierValue());
        assertEquals(Status.SUCCESS, latest.getStatus());
        assertNull(latest.getReason());
        assertNotNull(latest.getAttemptedAt());
    }

    @Test
    public void theMostRecentAttemptWins() throws Exception {
        service.record(context, item, "crossref", DOI_FIELD, DOI, Status.ERROR, "HTTP 500");
        service.record(context, item, "crossref", DOI_FIELD, DOI, Status.SUCCESS, null);

        assertEquals(Status.SUCCESS, service.findLatest(context, item, "crossref", DOI).getStatus());
    }

    @Test
    public void attemptsAreScopedByProviderAndIdentifier() throws Exception {
        service.record(context, item, "crossref", DOI_FIELD, DOI, Status.SUCCESS, null);

        assertNull(service.findLatest(context, item, "pubmed", DOI));
        assertNull(service.findLatest(context, item, "crossref", "10.1000/other"));
    }

    @Test
    public void reasonIsTruncatedToTheColumnSize() throws Exception {
        String reason = "x".repeat(5000);
        service.record(context, item, "crossref", DOI_FIELD, DOI, Status.ERROR, reason);

        assertEquals(1024, service.findLatest(context, item, "crossref", DOI).getReason().length());
    }

    @Test
    public void onlyErrorsAreRetriedAndOnlyAfterTheDelay() throws Exception {
        assertTrue("never attempted", service.shouldAttempt(context, item, "crossref", DOI));

        service.record(context, item, "crossref", DOI_FIELD, DOI, Status.SUCCESS, null);
        assertFalse("already succeeded", service.shouldAttempt(context, item, "crossref", DOI));

        service.record(context, item, "crossref", DOI_FIELD, DOI, Status.NOT_FOUND, null);
        assertFalse("the source does not know the identifier", service.shouldAttempt(context, item, "crossref", DOI));

        service.record(context, item, "crossref", DOI_FIELD, DOI, Status.ERROR, "HTTP 500");
        setRetryDelay("3600");
        assertFalse("failed too recently", service.shouldAttempt(context, item, "crossref", DOI));
        setRetryDelay("0");
        assertTrue("delay elapsed", service.shouldAttempt(context, item, "crossref", DOI));
    }

    @Test
    public void attemptsAreDeletedWithTheItem() throws Exception {
        UUID itemUuid = item.getID();
        service.record(context, item, "crossref", DOI_FIELD, DOI, Status.SUCCESS, null);
        context.commit();

        ItemBuilder.deleteItem(itemUuid);

        assertNull(dao.findLatest(context, itemUuid, "crossref", DOI));
    }
}
