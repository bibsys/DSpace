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
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.uclouvain.content.snapshot.ItemSnapshot;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.diff.formats.OutputFormat;
import org.dspace.uclouvain.content.snapshot.element.FileSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.MetadataSnapshotElement;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.plugins.UCLouvainAccessStatusHelper;
import org.dspace.uclouvain.services.ItemSnapshotService;
import org.junit.Before;
import org.junit.Test;

public class ItemSnapshotServiceTest extends AbstractIntegrationTestWithDatabase {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
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
        String pub1Author4 = "Author#4";
        String pub1Title = "Publication#1 : Title";
        String pub1Subject1 = "keyword#A1";
        String pub1Abstract = "Lorem ipsum abstract publication#1";
        String pub1Bitstream1Name = "Bitstream#1";
        String pub1Bitstream1NameModified = "Bitstream#1.1";
        String pub1Bitstream1Content = "TEST éàç°£ TEST";

        String loremIpsumText =   // 2 paragraphs, 189 words, 1320 bytes
            """
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin vel tortor sit amet ante cursus mattis. \
            Sed diam tortor, sodales quis dolor sed, elementum egestas massa. Nullam mollis venenatis nisi id \
            lacinia. Proin varius diam sed nunc luctus mollis. Phasellus consectetur convallis metus, ut eleifend \
            elit iaculis non. Mauris varius mattis urna, ut hendrerit augue pellentesque eu. Curabitur id iaculis \
            orci. Nullam blandit erat vel elementum tristique. Nam quis imperdiet ante. Praesent interdum sodales \
            ex. Quisque a lacinia dolor. Suspendisse in ex lorem. Nulla aliquam mauris mi, vitae posuere lectus \
            venenatis vestibulum. Aliquam vitae aliquet arcu, ac molestie dolor. Maecenas facilisis vehicula massa \
            id consequat.

            Integer sodales luctus suscipit. Vestibulum placerat bibendum urna. Cras id lorem in ligula hendrerit \
            commodo. Integer viverra euismod nulla, non fermentum est pulvinar sed. Vivamus feugiat lectus sed \
            condimentum consequat. Mauris at lobortis dolor. Pellentesque ultrices turpis eget quam tincidunt \
            laoreet. Curabitur nec fermentum lectus, in accumsan libero. Cras posuere non urna a dictum. Fusce eu \
            nisi ipsum. Suspendisse orci lorem, convallis quis viverra in, semper non arcu. Phasellus blandit \
            pulvinar tincidunt. Aliquam porta orci id egestas accumsan. Cras consectetur at ante at venenatis.""";

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

        // It's now time to test real tracked changes....
        //    1) Add a new tracked metadata (dc.contributor.author) and persist changes.
        //       Detect this changes is well known as "ADD"
        //    2) Delete this newly created author and create a new on (and persist changes again)
        //       Detect this changes is well known as "UPDATE" (delete + create same metadata)
        //    3) Update content of this last created author and place a large "lorem ipsum" text into it
        //       Take a snapshot and store it into DB
        //       Reload the entity and update this metadata to ADD/REMOVE/UPDATE some words
        //       This is to test the correct RawFormatter explanations
        //    4) Finally delete this metadata and check and persist changes.
        //       Detect this changes is well known as "DELETE"
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
        String explanations = snapshotService.explainChanges(snapshotDiff, OutputFormat.RAW);
        assertThat(explanations, allOf(containsString("[[+"), containsString(pub1Author3)));
        snapshotService.store(context, snapshotService.takeSnapshot(context, item1.getID()));

        context.turnOffAuthorisationSystem();
        MetadataValue mdValue = itemService.getMetadataByMetadataStringAndPlace(item1, "dc.contributor.author", 2);
        itemService.removeMetadataValues(context, item1, List.of(mdValue));
        itemService.addMetadata(context, item1, "dc", "contributor", "author", null, pub1Author4);
        itemService.update(context, item1);
        context.restoreAuthSystemState();
        context.commit();
        item1 = context.reloadEntity(item1);
        snapshotDiff = snapshotService.detectChanges(context, item1.getID());
        assertTrue(snapshotDiff.hasChanges());
        assertEquals(1, snapshotDiff.getChanges().size());
        assertNotNull(snapshotDiff.getChange("dc.contributor.author[2]"));
        explanations = snapshotService.explainChanges(snapshotDiff, OutputFormat.RAW);
        assertThat(explanations, allOf(
            containsString("[[~"),
            containsString(pub1Author3),
            containsString("->"),
            containsString(pub1Author4)
        ));

        context.turnOffAuthorisationSystem();
        itemService.replaceMetadata(context, item1, "dc", "contributor", "author", null, loremIpsumText, null, 0, 2);
        itemService.update(context, item1);
        snapshotService.store(context, snapshotService.takeSnapshot(context, item1));
        context.restoreAuthSystemState();
        context.commit();
        item1 = context.reloadEntity(item1);
        mdValue = itemService.getMetadataByMetadataStringAndPlace(item1, "dc.contributor.author", 2);
        // build new complex string
        //    * add "foo bar" after the words#4
        //    * update words#89-92 by "this a new fake data with much more words"
        //    * delete words#141-142
        String newWords = "foo bar";
        String updatedWords = "this a new fake data with much more words";
        List<String> words = new ArrayList<>(Arrays.asList(mdValue.getValue().split("\\s+")));
        String deletedWords = String.join(" ", words.get(141), words.get(142));
        words.subList(141, 143).clear();
        words.subList(89, 93).clear();
        words.addAll(89, List.of(updatedWords.split("\\s+")));
        words.addAll(4, List.of(newWords.split("\\s+")));
        context.turnOffAuthorisationSystem();
        mdValue = itemService.getMetadataByMetadataStringAndPlace(item1, "dc.contributor.author", 2);
        itemService.removeMetadataValues(context, item1, List.of(mdValue));
        itemService.addMetadata(context, item1, "dc", "contributor", "author", null, String.join(" ", words));
        itemService.update(context, item1);
        context.restoreAuthSystemState();
        context.commit();
        item1 = context.reloadEntity(item1);
        snapshotDiff = snapshotService.detectChanges(context, item1.getID());
        assertTrue(snapshotDiff.hasChanges());
        assertEquals(1, snapshotDiff.getChanges().size());
        assertNotNull(snapshotDiff.getChange("dc.contributor.author[2]"));
        explanations = snapshotService.explainChanges(snapshotDiff, OutputFormat.RAW);
        // Build regexp to match correct explanation
        String newPattern = Pattern.quote("[[+ %s]]".formatted(newWords));
        String updatePattern = Pattern.quote("[[~ ") + ".*" + Pattern.quote(" -> %s]]".formatted(updatedWords));
        String deletePattern = Pattern.quote("[[- %s]]".formatted(deletedWords));
        String fullRegexPattern = "(?s).*%s.*%s.*%s.*".formatted(newPattern, updatePattern, deletePattern);
        assertThat(explanations, matchesPattern(fullRegexPattern));
        snapshotService.store(context, snapshotService.takeSnapshot(context, item1));

        context.turnOffAuthorisationSystem();
        mdValue = itemService.getMetadataByMetadataStringAndPlace(item1, "dc.contributor.author", 2);
        itemService.removeMetadataValues(context, item1, List.of(mdValue));
        itemService.update(context, item1);
        context.restoreAuthSystemState();
        context.commit();
        item1 = context.reloadEntity(item1);
        snapshotDiff = snapshotService.detectChanges(context, item1.getID());
        assertTrue(snapshotDiff.hasChanges());
        assertEquals(1, snapshotDiff.getChanges().size());
        explanations = snapshotService.explainChanges(snapshotDiff, OutputFormat.RAW);
        assertThat(explanations, containsString("dc.contributor.author[2] :: [[-"));
        snapshotService.store(context, snapshotService.takeSnapshot(context, item1));

        // Now change bitstream attached to Item1 and test snapshot
        //   * Changing "dc.title" bitstream metadata
        //   * DSpace doesn't allow to update a bitstream content... we need to delete it and create a new one.
        //     So check on checksum is just well to know if bitstream integrity is correct
        //   * Update the bitstream access type is too heavy for this test (if reviewer want this test, it can write it)
        // At the end, delete the bitstream and test snapshot again
        bitstream = context.reloadEntity(bitstream);
        context.turnOffAuthorisationSystem();
        bitstreamService.replaceMetadata(
                context, bitstream, "dc", "title", null, null, pub1Bitstream1NameModified, null, 0, 0);
        bitstreamService.update(context, bitstream);
        context.restoreAuthSystemState();
        context.commit();
        bitstream = context.reloadEntity(bitstream);
        item1 = context.reloadEntity(item1);
        snapshotDiff = snapshotService.detectChanges(context, item1.getID());
        assertTrue(snapshotDiff.hasChanges());
        assertEquals(1, snapshotDiff.getChanges().size());
        explanations = snapshotService.explainChanges(snapshotDiff, OutputFormat.RAW);
        String expectedMessage = "The file [%s] has been updated :: filename [%s --> %s]".formatted(
            pub1Bitstream1NameModified,
            pub1Bitstream1Name,
            pub1Bitstream1NameModified
        );
        assertEquals(expectedMessage, explanations);
        snapshotService.store(context, snapshotService.takeSnapshot(context, item1));

        context.turnOffAuthorisationSystem();
        bitstreamService.delete(context, bitstream);
        context.restoreAuthSystemState();
        context.commit();
        item1 = context.reloadEntity(item1);
        snapshotDiff = snapshotService.detectChanges(context, item1.getID());
        assertTrue(snapshotDiff.hasChanges());
        assertEquals(1, snapshotDiff.getChanges().size());
        explanations = snapshotService.explainChanges(snapshotDiff, OutputFormat.RAW);
        expectedMessage = "The file [%s] has been removed from the publication.".formatted(pub1Bitstream1NameModified);
        System.out.println("Expected    :: " + expectedMessage);
        System.out.println("Explanation :: " + explanations);
        assertThat(explanations, containsString(expectedMessage));

        // Detect changes on item2.
        //    This test must fail because we don't store any ItemSnapshot for item2.
        //    So `takeSnapshot` + `compareSnapshot` could work; but `detectChanges` must raise exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> snapshotService.detectChanges(context, item2)
        );
        assertThat(exception.getMessage(), is("Unable to retrieve snapshot for Item#" + item2.getID()));
        UUID randomUUID = UUID.randomUUID();
        exception = assertThrows(
            IllegalArgumentException.class,
            () -> snapshotService.detectChanges(context, randomUUID)
        );
        assertThat(exception.getMessage(), is("Item#" + randomUUID + " doesn't exist"));
    }

}
