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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import org.dspace.uclouvain.content.snapshot.ItemSnapshot;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.element.FileSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.MetadataSnapshotElement;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.plugins.UCLouvainAccessStatusHelper;
import org.dspace.uclouvain.services.ItemSnapshotService;
import org.junit.Before;
import org.junit.Test;

public class ItemSnapshotServiceTest extends AbstractIntegrationTestWithDatabase {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private ItemSnapshotService snapshotService;
    private Collection collection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        snapshotService = UCLouvainServiceFactory.getInstance().getSnapshotService();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Global")
            .build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testItemSnapshot1() throws Exception {
        String pub1Author1 = "Author#1";
        String pub1Author2 = "Author#2";
        String pub1Author3 = "Author#3";
        String pub1Title = "Publication#1 : Title";
        String pub1Subject1 = "keyword#A1";
        String pub1Abstract = "Lorem ipsum abstract publication#1";
        String pub1Bitstream1Name = "Bitstream#1";
        String pub1Bitstream1Content = "TEST éàç°£ TEST";

        String pub2Title = "Publication#2 : OtherTitle";

        // Create sample items
        //   ITEM#1
        //     with some relevant (author, title, peer-review) and some not-relevant metadata for snapshot
        //     with one attached bitstream (no restriction)
        //   ITEM#2
        //     with some relavant (title) and some not-relevant metadata
        //     with no file
        context.turnOffAuthorisationSystem();
        Item item1 = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withAuthor(pub1Author1)
            .withAuthor(pub1Author2)
            .withTitle(pub1Title)
            .withSubject(pub1Subject1)
            .withDescriptionAbstract(pub1Abstract)
            .withMetadata("publication", "serial", "peerReviewed", "true")
            .build();
        Bitstream bitstream = BitstreamBuilder
            .createBitstream(context, item1, toInputStream(pub1Bitstream1Content, UTF_8))
            .withName(pub1Bitstream1Name)
            .withMimeType("text/plain")
            .build();

        Item item2 = ItemBuilder.createItem(context, collection)
            .withTitle(pub2Title)
            .build();

        context.restoreAuthSystemState();
        context.commit();

        // Create the snapshot for this item
        //    Check the snapshot should contain 5 SnapshotElement (4 metadata, 1 file)
        //    Check the snapshot for "dc.contributor.author[1]" path, the value is equals to `pub1Author2`
        //    Check the snapshot related to bitstream as `openAccess` as access restriction, and correct size
        ItemSnapshot snapshot = snapshotService.takeSnapshot(context, item1.getID());
        assertNotNull(snapshot);
        assertEquals(5, snapshot.getSnapshotElements().size());

        MetadataSnapshotElement sEl1 = (MetadataSnapshotElement)snapshot.getSnapshotElement("dc.contributor.author[1]");
        assertEquals(pub1Author2, sEl1.getValue());

        FileSnapshotElement sEl2 = (FileSnapshotElement)snapshot.getSnapshotElement(bitstream.getID().toString());
        assertEquals(UCLouvainAccessStatusHelper.OPEN_ACCESS, sEl2.getAccess());

        // Store the snapshot into the database, and get it from database.
        //    First store the freshly created snapshot
        //    Then try to load snapshot for `item2`: No snapshot was saved for this item, snapshot must be null
        //    Then try to load snapshot for `item1`: The previously saved snapshot should be returned
        snapshotService.store(context, snapshot);
        context.commit();
        context.dispatchEvents();

        snapshot = snapshotService.get(context, item2.getID());
        assertNull(snapshot);

        snapshot = snapshotService.get(context, item1.getID());
        assertNotNull(snapshot);
        assertEquals(5, snapshot.getSnapshotElements().size());

        // Now, add a new irrelevant metadata into the item.
        // Store/persist this change into the database and take a new snapshot for this item
        // Ask service to detect changes --> No changes should be detected between both item snapshots
        context.turnOffAuthorisationSystem();
        item1 = context.reloadEntity(item1);
        itemService.addMetadata(context, item1, "fedora", "pid", null, null, "legacyFedoraPid");
        itemService.update(context, item1);
        context.restoreAuthSystemState();
        context.commit();
        item1 = context.reloadEntity(item1);
        assertTrue(itemService.hasMetadata(item1, "fedora", "pid", null));

        ItemSnapshot snapshot2 = snapshotService.takeSnapshot(context, item1);
        ItemSnapshotDiff snapshotDiff = snapshotService.compareSnapshot(snapshot, snapshot2);
        assertFalse(snapshotDiff.hasChanges());

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, item1, "dc", "contributor", "author", null, pub1Author3);
        itemService.update(context, item1);
        context.restoreAuthSystemState();
        context.commit();
        item1 = context.reloadEntity(item1);
        snapshotDiff = snapshotService.detectChanges(context, item1);
        assertTrue(snapshotDiff.hasChanges());
        assertEquals(1, snapshotDiff.getChanges().size());
        assertNotNull(snapshotDiff.getChange("dc.contributor.author[2]"));
    }

}
