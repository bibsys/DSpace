/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff.explainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import org.dspace.uclouvain.content.snapshot.diff.DiffReport;
import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.element.MetadataSnapshotElement;

/**
 * Explainer class for diff change on a publication metadata.
 * This utility class should be used to detect/get changes and render the change in the desired format
 * (see {@link org.dspace.uclouvain.content.snapshot.diff.formats.DiffFormatter}) for better usage comprehension
 *
 * @author Renaud Michptte (renaud.michotte@uclouvain.be)
 */
@DiffExplainerFor(MetadataSnapshotElement.class)
public class MetadataDiffExplainer extends DiffExplainer<MetadataSnapshotElement> {

    private static final int CONTEXT_SIZE = 5;

    public MetadataDiffExplainer(MetadataSnapshotElement original, MetadataSnapshotElement revised)
        throws IllegalArgumentException {
        super(original, revised);
    }

    /**
     * Get the appropriate (regarding change type) diff report to used to render this change
     * @return the diff report
     */
    public DiffReport getDiff() {
        return switch (getType()) {
            case ItemSnapshotDiff.ADD -> getAddReport();
            case ItemSnapshotDiff.REMOVE -> getRemoveReport();
            case ItemSnapshotDiff.UPDATE -> getUpdateReport();
            default -> throw new IllegalStateException("Unexpected type: " + getType());
        };
    }

    /**
     * Get the {@link DiffReport} for a newly added metadata.
     * This is a simple case where all revised metadata value is a INSERTED segments.
     * @return the diff report that can be formatted
     */
    private DiffReport getAddReport() {
        DiffReport.DiffSegment segment = new DiffReport.DiffSegment(
            DiffReport.DiffSegment.Type.INSERTED,
            getRevised().getValue()
        );
        DiffReport.DiffBlock block = new DiffReport.DiffBlock(List.of(segment));
        return new DiffReport(List.of(block));
    }

    /**
     * Get the {@link DiffReport} for a previously existing metadata that has been removed.
     * This is a simple case where all original metadata value is a DELETED segments.
     * @return the diff report that can be formatted
     */
    private DiffReport getRemoveReport() {
        DiffReport.DiffSegment segment = new DiffReport.DiffSegment(
            DiffReport.DiffSegment.Type.DELETED,
            getOriginal().getValue()
        );
        DiffReport.DiffBlock block = new DiffReport.DiffBlock(List.of(segment));
        return new DiffReport(List.of(block));
    }

    /**
     * Get the {@link DiffReport} when metadata value changes.
     * This is the most complex case because we need to find contextual changes into possible large metadata value
     * @return the diff report that can be formatted
     */
    private DiffReport getUpdateReport() {
        // 1. Tokenize by words (splitting on spaceS)
        List<String> origWords = Arrays.asList(getOriginal().getValue().split("\\s+"));
        List<String> revWords = Arrays.asList(getRevised().getValue().split("\\s+"));

        // 2. Compute Diff based on words collections
        Patch<String> patch = DiffUtils.diff(origWords, revWords);
        List<AbstractDelta<String>> deltas = patch.getDeltas();
        // DEV NOTE ::
        //   it should never happen because we already checked that original/revised values has been modify...
        //   just to be "java safe"
        if (deltas.isEmpty()) {
            return new DiffReport(Collections.emptyList());
        }

        // 3. Determining capture zones (word intervals in the original text) and merge overlapping or adjacent areas
        List<int[]> zones = new ArrayList<>();
        for (AbstractDelta<String> delta : deltas) {
            int startIdx = Math.max(0, delta.getSource().getPosition() - CONTEXT_SIZE);
            // The end of the zone encompasses the delta and the surrounding area beyond
            int endIdx = Math.min(
                origWords.size(),
                delta.getSource().getPosition() + delta.getSource().getLines().size() + CONTEXT_SIZE
            );
            zones.add(new int[]{startIdx, endIdx});
        }
        List<int[]> mergedZones = mergeZones(zones);

        // 4. Build {@link DiffReport.DiffBlock} list
        List<DiffReport.DiffBlock> blocks = new ArrayList<>();
        int currentDeltaIdx = 0;
        for (int[] zone : mergedZones) {
            int zoneStart = zone[0];
            int zoneEnd = zone[1];

            List<DiffReport.DiffSegment> segments = new ArrayList<>();
            int cursor = zoneStart;
            // Loop on deltas of the zone
            while (currentDeltaIdx < deltas.size()) {
                AbstractDelta<String> delta = deltas.get(currentDeltaIdx);
                int deltaPos = delta.getSource().getPosition();
                int deltaSize = delta.getSource().getLines().size();
                if (deltaPos >= zoneEnd) {
                    break;
                }
                // unchanged text before delta (Context)
                if (deltaPos > cursor) {
                    String contextText = String.join(" ", origWords.subList(cursor, deltaPos));
                    segments.add(new DiffReport.DiffSegment(DiffReport.DiffSegment.Type.CONTEXT, contextText));
                }
                // changes management
                if (delta.getType() == DeltaType.CHANGE ) {
                    String oText = String.join(" ", delta.getSource().getLines());
                    String rText = String.join(" ", delta.getTarget().getLines());
                    segments.add(new DiffReport.DiffSegment(DiffReport.DiffSegment.Type.UPDATED, oText, rText));
                } else if (delta.getType() == DeltaType.DELETE ) {
                    String text = String.join(" ", delta.getSource().getLines());
                    segments.add(new DiffReport.DiffSegment(DiffReport.DiffSegment.Type.DELETED, text));
                } else if (delta.getType() == DeltaType.INSERT) {
                    String text = String.join(" ", delta.getTarget().getLines());
                    segments.add(new DiffReport.DiffSegment(DiffReport.DiffSegment.Type.INSERTED, text));
                }
                cursor = deltaPos + deltaSize;
                currentDeltaIdx++;
            }
            // unchanged text until end of zone (Context)
            if (zoneEnd > cursor) {
                String contextText = String.join(" ", origWords.subList(cursor, zoneEnd));
                segments.add(new DiffReport.DiffSegment(DiffReport.DiffSegment.Type.CONTEXT, contextText));
            }
            blocks.add(new DiffReport.DiffBlock(segments));
        }
        return new DiffReport(blocks);

    }

    /**
     * Analyze a list of zones to determine if some of them should be merges because they overlapped/adjacent.
     * @param zones the list of zones to analyze
     * @return the final list of zones to create a {@link DiffReport}
     */
    private List<int[]> mergeZones(List<int[]> zones) {
        if (zones.isEmpty()) {
            return zones;
        }
        zones.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> merged = new ArrayList<>();
        int[] current = zones.get(0);
        merged.add(current);
        for (int[] next : zones) {
            // If the start of the next zone intersects or touches the end of the current zone
            if (next[0] <= current[1]) {
                current[1] = Math.max(current[1], next[1]); // Fusion !!!
            } else {
                current = next;
                merged.add(current);
            }
        }
        return merged;
    }

}
