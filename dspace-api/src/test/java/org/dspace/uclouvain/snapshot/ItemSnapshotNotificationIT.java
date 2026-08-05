/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.snapshot;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.diff.formats.OutputFormat;
import org.dspace.uclouvain.content.snapshot.element.MetadataSnapshotElement;
import org.dspace.uclouvain.core.NotificationType;
import org.dspace.uclouvain.core.mails.Recipient;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.ItemSnapshotService;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests about the resilience of the changes notification.
 *
 * DEV NOTE :: the test environment runs with `mail.server.disabled = true`, so `Email.send()` still builds and renders
 *             the whole message (Velocity template included) but never reaches an SMTP server.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class ItemSnapshotNotificationIT extends AbstractIntegrationTestWithDatabase {

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private ItemSnapshotService snapshotService;
    private Collection collection;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        snapshotService = UCLouvainServiceFactory.getInstance().getSnapshotService();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity).withName("Global").build();
        context.restoreAuthSystemState();
    }

    /**
     * NON-REGRESSION TEST :: one unexplainable publication must not discard the whole notification.
     *
     * The explanations used to be built by a single stream, so the first item raising anything (unknown citation
     * crosswalk, missing i18n key, unexpected metadata value, ...) aborted `notifyRecipient` altogether. The recipient
     * then also lost the changes detected on all its other publications, which were perfectly formattable.
     */
    @Test
    public void testOneUnexplainableItemDoesNotDiscardTheWholeNotification() throws Exception {
        ItemSnapshotDiff explainableDiff = buildExplainableDiff();
        ItemSnapshotDiff unexplainableDiff = buildUnexplainableDiff();

        // Guard the injection itself: should this diff become explainable, the test would silently stop covering
        // anything, so make that visible instead.
        assertThrows(
            Exception.class,
            () -> snapshotService.explainChanges(unexplainableDiff, OutputFormat.EMAIL_HTML, Locale.ENGLISH)
        );

        Recipient recipient = new Recipient("Doe, John", Map.of(NotificationType.EMAIL, "john.doe@uclouvain.be"));

        // The faulty publication is skipped and logged, the sound one is still notified: this must NOT throw.
        snapshotService.notifyRecipient(
            context, recipient, List.of(unexplainableDiff, explainableDiff), NotificationType.EMAIL
        );
    }

    /**
     * When NO publication of the batch can be explained, no empty notification may be sent, and the failure must stay
     * contained (logged, not propagated).
     */
    @Test
    public void testNotificationIsSkippedWhenNothingCanBeExplained() throws Exception {
        ItemSnapshotDiff unexplainableDiff = buildUnexplainableDiff();
        Recipient recipient = new Recipient("Doe, John", Map.of(NotificationType.EMAIL, "john.doe@uclouvain.be"));

        snapshotService.notifyRecipient(context, recipient, List.of(unexplainableDiff), NotificationType.EMAIL);
    }

    // PRIVATE METHODS =================================================================================================
    /** Build a real, perfectly formattable diff: a tracked metadata added to an already snapshotted item */
    private ItemSnapshotDiff buildExplainableDiff() throws Exception {
        UUID itemId = createItem("Publication#sound");
        snapshotService.store(context, snapshotService.takeSnapshot(context, itemId));
        context.commit();

        context.turnOffAuthorisationSystem();
        Item item = itemService.find(context, itemId);
        itemService.addMetadata(context, item, "dc", "contributor", "author", null, "Doe, John");
        itemService.update(context, item);
        context.restoreAuthSystemState();
        context.commit();

        ItemSnapshotDiff diff = snapshotService.detectChanges(context, itemId);
        assertTrue("the sound diff must hold a change to be meaningful", diff.hasChanges());
        return diff;
    }

    /**
     * Build a diff whose explanation blows up.
     * A metadata updated to a null value makes the word-by-word comparison fail, which stands here for any
     * unexpected value that a formatter cannot digest.
     */
    private ItemSnapshotDiff buildUnexplainableDiff() throws Exception {
        UUID itemId = createItem("Publication#faulty");
        ItemSnapshotDiff diff = new ItemSnapshotDiff(itemService.find(context, itemId));
        diff.addChange(
            new MetadataSnapshotElement("dc.title[0]", "a title that was there before"),
            new MetadataSnapshotElement("dc.title[0]", null)
        );
        return diff;
    }

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
}
