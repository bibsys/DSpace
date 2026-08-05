/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.element.FileSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.MetadataSnapshotElement;
import org.junit.Test;

/**
 * Unit tests about how an {@link ItemSnapshotDiff} indexes and orders the changes it holds.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class ItemSnapshotDiffTest {

    private ItemSnapshotDiff emptyDiff() {
        return new ItemSnapshotDiff(UUID.randomUUID());
    }

    /** The order the changes are rendered in, i.e. what `ItemSnapshotService#explainChanges` applies */
    private List<String> renderedPathOrder(ItemSnapshotDiff diff) {
        return diff.getChanges().stream()
            .sorted(Comparator.comparing(ItemSnapshotDiff::getChangeSortKey))
            .map(change -> (change.getLeft() != null ? change.getLeft() : change.getRight()).getPath())
            .toList();
    }

    /**
     * NON-REGRESSION TEST :: occurrences must be ordered numerically, not lexicographically.
     *
     * The sort key used to be the raw path, so a publication with ten authors or more listed them as
     * [0], [1], [10], [11], [2]... in the notification e-mail.
     */
    @Test
    public void testOccurrencesAreOrderedNumerically() {
        ItemSnapshotDiff diff = emptyDiff();
        for (int index : new int[] {2, 10, 0, 11, 1}) {
            diff.addChange(null, new MetadataSnapshotElement("dc.contributor.author[" + index + "]", "Doe, John"));
        }

        assertEquals(
            List.of("dc.contributor.author[0]", "dc.contributor.author[1]", "dc.contributor.author[2]",
                "dc.contributor.author[10]", "dc.contributor.author[11]"),
            renderedPathOrder(diff)
        );
    }

    /** A path carrying no occurrence index (a bitstream one) must still be orderable, without blowing up */
    @Test
    public void testPathsWithoutOccurrenceIndexAreSupported() {
        ItemSnapshotDiff diff = emptyDiff();
        diff.addChange(null, new FileSnapshotElement(UUID.randomUUID(), "report.pdf", "MD5#abc", "openaccess"));
        diff.addChange(null, new MetadataSnapshotElement("dc.title[0]", "Lorem ipsum"));

        assertEquals(2, renderedPathOrder(diff).size());
    }

    /**
     * NON-REGRESSION TEST :: two elements of different types sharing a path must both be kept.
     *
     * The comparison keys elements by type AND path, but the diff used to index them by path alone, so one silently
     * overwrote the other -- losing a detected change.
     */
    @Test
    public void testTwoElementTypesSharingAPathDoNotOverwriteEachOther() {
        UUID bitstreamId = UUID.randomUUID();
        ItemSnapshotDiff diff = emptyDiff();

        diff.addChange(null, new FileSnapshotElement(bitstreamId, "report.pdf", "MD5#abc", "openaccess"));
        diff.addChange(null, new MetadataSnapshotElement(bitstreamId.toString(), "an homonymous metadata"));

        assertEquals("both changes must be kept", 2, diff.getChanges().size());
        assertNotNull("and still be reachable by path", diff.getChange(bitstreamId.toString()));
    }

    /** Re-adding a change on the same element replaces it rather than duplicating it */
    @Test
    public void testAddingTwiceTheSameElementReplacesTheChange() {
        ItemSnapshotDiff diff = emptyDiff();
        diff.addChange(null, new MetadataSnapshotElement("dc.title[0]", "first"));
        diff.addChange(null, new MetadataSnapshotElement("dc.title[0]", "second"));

        assertEquals(1, diff.getChanges().size());
        assertEquals("second", ((MetadataSnapshotElement) diff.getChange("dc.title[0]").getRight()).getValue());
    }
}
