/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.snapshot;

import static org.apache.commons.codec.CharEncoding.UTF_8;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.uclouvain.content.dao.ItemSnapshotDAO;
import org.dspace.uclouvain.content.snapshot.ItemSnapshot;
import org.dspace.uclouvain.content.snapshot.element.FileSnapshotElement;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.ItemSnapshotService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests about {@link ItemSnapshot} persistence: the timestamp actually written to the database, and the
 * {@link ItemSnapshotDAO#findItemsToSnapshot} eligibility rules that rely on it.
 *
 * DEV NOTE :: items are always handled by UUID rather than by entity, so the service reloads them from the current
 *             session; test helpers commit the context and would otherwise leave detached entities behind.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class ItemSnapshotDAOIT extends AbstractIntegrationTestWithDatabase {

    private static final Date PAST = Date.from(Instant.parse("2000-01-01T00:00:00Z"));
    private static final Date LATER = Date.from(Instant.parse("2010-01-01T00:00:00Z"));
    private static final Date FUTURE = Date.from(Instant.parse("2100-01-01T00:00:00Z"));

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private ItemSnapshotService snapshotService;
    private ItemSnapshotDAO snapshotDAO;
    private Collection collection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        snapshotService = UCLouvainServiceFactory.getInstance().getSnapshotService();
        snapshotDAO = new DSpace().getServiceManager().getApplicationContext().getBean(ItemSnapshotDAO.class);
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Global").build();
        context.restoreAuthSystemState();
    }

    /**
     * An archived item without any stored snapshot must always be eligible; once its snapshot is stored it must not be
     * returned anymore (the item wasn't modified in the meantime).
     */
    @Test
    public void testItemWithoutSnapshotIsEligible() throws Exception {
        UUID itemId = createItem("Publication#1");
        assertThat(snapshotService.findItemsToSnapshot(context, null, -1), hasItem(itemId));
        takeAndStoreSnapshot(itemId);
        assertThat(snapshotService.findItemsToSnapshot(context, null, -1), not(hasItem(itemId)));
    }

    /**
     * NON-REGRESSION TEST :: a snapshot timestamp must mirror `item.lastModified` on INSERT too.
     *
     * The column used to be mapped `insertable = false` and to carry a `DEFAULT NOW()`, so the value assigned by the
     * service was silently dropped and replaced by the row insertion time. The first snapshot of every item therefore
     * claimed to describe a state more recent than the one it actually held.
     */
    @Test
    public void testTimestampMirrorsItemLastModifiedOnInsert() throws Exception {
        UUID itemId = createItem("Publication#1");
        forceItemLastModified(itemId, PAST);

        takeAndStoreSnapshot(itemId);

        assertEquals(PAST.getTime(), readStoredTimestamp(itemId).getTime());
    }

    /**
     * The same invariant must hold when an existing snapshot gets refreshed (UPDATE path).
     */
    @Test
    public void testTimestampMirrorsItemLastModifiedOnUpdate() throws Exception {
        UUID itemId = createItem("Publication#1");
        forceItemLastModified(itemId, PAST);
        takeAndStoreSnapshot(itemId);

        forceItemLastModified(itemId, LATER);
        takeAndStoreSnapshot(itemId);

        assertEquals(LATER.getTime(), readStoredTimestamp(itemId).getTime());
    }

    /**
     * NON-REGRESSION TEST :: staleness must be evaluated against the timestamp of the item's OWN snapshot.
     *
     * A previous implementation compared `item.lastModified` to a global boundary (the most recent timestamp of the
     * whole `uclouvain_item_snapshot` table). With such an implementation, `staleItem` below is never returned because
     * its own modification date is older than `freshItem`'s snapshot timestamp, and its changes stay undetected
     * forever.
     */
    @Test
    public void testStalenessIsEvaluatedPerItem() throws Exception {
        UUID staleItemId = createItem("Publication#stale");
        UUID freshItemId = createItem("Publication#fresh");

        takeAndStoreSnapshot(staleItemId);
        takeAndStoreSnapshot(freshItemId);

        // `staleItem` snapshot is outdated (taken long before the last item modification) whereas `freshItem` snapshot
        // is up-to-date. `freshItem` also carries the highest timestamp of the whole table.
        setSnapshotTimestamp(staleItemId, PAST);
        setSnapshotTimestamp(freshItemId, FUTURE);

        List<UUID> candidates = snapshotService.findItemsToSnapshot(context, null, -1);
        assertThat(candidates, hasItem(staleItemId));
        assertThat(candidates, not(hasItem(freshItemId)));
    }

    /**
     * When provided, the `from` argument only narrows the result; it never replaces the per-item comparison.
     */
    @Test
    public void testFromArgumentOnlyNarrowsResult() throws Exception {
        UUID itemId = createItem("Publication#1");

        takeAndStoreSnapshot(itemId);
        setSnapshotTimestamp(itemId, PAST);

        // The item is stale, and was modified after PAST: it is eligible.
        assertThat(snapshotService.findItemsToSnapshot(context, PAST, -1), hasItem(itemId));
        // The item is still stale, but wasn't modified after FUTURE: the caller explicitly excluded it.
        assertThat(snapshotService.findItemsToSnapshot(context, FUTURE, -1), not(hasItem(itemId)));
    }

    /**
     * Results are limited and ordered by ascending modification date, so that the oldest pending items are drained
     * first and no item can starve.
     */
    @Test
    public void testLimitAndOrdering() throws Exception {
        UUID firstId = createItem("Publication#1");
        UUID secondId = createItem("Publication#2");
        UUID thirdId = createItem("Publication#3");

        assertThat(snapshotService.findItemsToSnapshot(context, null, -1), contains(firstId, secondId, thirdId));

        List<UUID> limited = snapshotService.findItemsToSnapshot(context, null, 2);
        assertThat(limited, hasSize(2));
        assertEquals(firstId, limited.get(0));
        assertEquals(secondId, limited.get(1));
    }

    /**
     * NON-REGRESSION TEST :: a bitstream without a name must not abort the snapshot of its whole item.
     *
     * `FileSnapshotElement` used to build its attributes with `Map.of()`, which rejects null values. A bitstream
     * without `dc.title` therefore raised a NPE that propagated out of `takeSnapshot`, so the item was skipped AND
     * its snapshot never stored: it stayed eligible forever without ever converging, silently.
     */
    @Test
    public void testSnapshotSurvivesUnnamedBitstream() throws Exception {
        UUID itemId = createItemWithUnnamedBitstream();

        takeAndStoreSnapshot(itemId);

        // The item converged: it is not eligible anymore, and the file element round-tripped through the database.
        assertThat(snapshotService.findItemsToSnapshot(context, null, -1), not(hasItem(itemId)));
        assertThat(snapshotService.get(context, itemId).getSnapshotElementsOfType(FileSnapshotElement.class),
            hasSize(1));
    }

    /**
     * NON-REGRESSION TEST :: loading the same snapshot twice must not duplicate its elements.
     *
     * `snapshotElements` is @Transient on a Hibernate entity, so the session hands back the very same instance on a
     * second `get()` within one context. `deserialize` used to APPEND, which doubled every element and fabricated
     * phantom changes at comparison time.
     */
    @Test
    public void testLoadingASnapshotTwiceDoesNotDuplicateItsElements() throws Exception {
        UUID itemId = createItem("Publication#1");
        takeAndStoreSnapshot(itemId);

        int firstLoad = snapshotService.get(context, itemId).getSnapshotElements().size();
        int secondLoad = snapshotService.get(context, itemId).getSnapshotElements().size();
        assertEquals(firstLoad, secondLoad);

        // And the duplication must not surface as a change either.
        assertFalse(snapshotService.detectChanges(context, itemId).hasChanges());
    }

    // PRIVATE METHODS =================================================================================================
    private UUID createItem(String title) throws Exception {
        context.turnOffAuthorisationSystem();
        UUID itemId = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle(title)
            .build()
            .getID();
        context.restoreAuthSystemState();
        context.commit();
        return itemId;
    }

    /** Create an item holding a bitstream with NO `dc.title`, so that `Bitstream#getName()` returns null */
    private UUID createItemWithUnnamedBitstream() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Publication#with-unnamed-bitstream")
            .build();
        Bitstream bitstream = BitstreamBuilder
            .createBitstream(context, item, toInputStream("some content", UTF_8))
            .withMimeType("text/plain")
            .build();
        context.restoreAuthSystemState();
        context.commit();
        assertNull("the bitstream must stay unnamed for this test to cover anything",
            context.reloadEntity(bitstream).getName());
        return item.getID();
    }

    private void takeAndStoreSnapshot(UUID itemId) throws Exception {
        snapshotService.store(context, snapshotService.takeSnapshot(context, itemId));
        context.commit();
    }

    /**
     * Force the stored snapshot timestamp of an item, to simulate snapshots taken at arbitrary moments without having
     * to rely on the wall clock.
     */
    private void setSnapshotTimestamp(UUID itemId, Date timestamp) throws Exception {
        ItemSnapshot snapshot = snapshotDAO.findByID(context, ItemSnapshot.class, itemId);
        snapshot.setTimestamp(timestamp);
        snapshotDAO.save(context, snapshot);
        context.commit();
    }

    /**
     * Force the modification date of an item, to get a deterministic expected value instead of depending on how long
     * the test took to run.
     */
    private void forceItemLastModified(UUID itemId, Date lastModified) throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = itemService.find(context, itemId);
        item.setLastModified(lastModified);
        context.restoreAuthSystemState();
        context.commit();
    }

    /**
     * Read the timestamp really persisted for an item snapshot.
     * DEV NOTE :: the session cache MUST be cleared first, otherwise Hibernate returns the in-memory value assigned by
     *             the service and the test would pass even when nothing reached the database.
     */
    private Date readStoredTimestamp(UUID itemId) throws Exception {
        context.uncacheEntities();
        return snapshotDAO.findByID(context, ItemSnapshot.class, itemId).getTimestamp();
    }
}
