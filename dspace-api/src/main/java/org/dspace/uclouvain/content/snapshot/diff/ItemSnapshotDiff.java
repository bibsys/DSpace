/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.uclouvain.content.snapshot.element.SnapshotElement;

/**
 * This class represent difference found between two snapshot of the same item.
 * Any difference between tracked {@link org.dspace.uclouvain.content.snapshot.element.SnapshotElement} are listed
 * into it.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class ItemSnapshotDiff {

    // CLASS CONSTANTS =================================================================================================
    public static final String ADD = "add";
    public static final String REMOVE = "remove";
    public static final String UPDATE = "update";
    /** Matches the trailing occurrence index of a metadata path, e.g. the `[2]` of `dc.contributor.author[2]` */
    private static final Pattern OCCURRENCE_INDEX = Pattern.compile("^(.*)\\[(\\d{1,9})]$");

    // CLASS ATTRIBUTES ================================================================================================
    /**
     * The item these changes relate to, kept as a bare identifier.
     * DEV NOTE :: do NOT store the {@link Item} entity here. A diff outlives the transaction it was computed in (the
     *             task commits between two items, and notifications are sent at the very end of the run), and Hibernate
     *             runs with a `ThreadLocalSessionContext` which closes the session on commit. A retained entity would
     *             therefore be detached, and reading any lazy relation on it would blow up. Callers needing the entity
     *             resolve it from the current context, which is always safe.
     */
    private final UUID itemId;
    /**
     * Store changes and index them.
     *   If left element exists but not right element --> this is a remove
     *   If left element is null, but right element exists --> this is an add
     *   If both element exists --> this is an update (!!! both element MUST share same path)
     */
    private final Map<String, Pair<SnapshotElement, SnapshotElement>> changes;

    // CLASS CONSTRUCTOR ===============================================================================================
    public ItemSnapshotDiff(Item item) {
        this(item.getID());
    }
    public ItemSnapshotDiff(UUID itemId) {
        this.itemId = itemId;
        this.changes = new HashMap<>();
    }

    // STATIC METHOD ===================================================================================================

    /**
     * Allow to compute a key to sort a change
     * @param change the change to analyze
     * @return a key to use to compare this change to other changes (using a classic Comparator)
     */
    public static String getChangeSortKey(Pair<SnapshotElement, SnapshotElement> change) {
        SnapshotElement testedElt = (change.getLeft() != null) ? change.getLeft() : change.getRight();
        Matcher matcher = OCCURRENCE_INDEX.matcher(testedElt.getPath());
        String testedEltName = matcher.matches()
            ? "%s[%09d]".formatted(matcher.group(1), Integer.parseInt(matcher.group(2)))
            : testedElt.getPath();
        return testedElt.getClass().getName() + "::" + testedEltName;
    }

    /** Get the element carrying the information of a change: the original one, or the revised one for an addition */
    private static SnapshotElement elementOf(Pair<SnapshotElement, SnapshotElement> change) {
        return (change.getLeft() != null) ? change.getLeft() : change.getRight();
    }


    // CLASS METHODS ===================================================================================================
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    // GETTER & SETTER =================================================================================================
    public UUID getItemId() {
        return itemId;
    }
    public void addChange(SnapshotElement original, SnapshotElement revised) {
        if (original != null && revised != null && !Objects.equals(original.getPath(), revised.getPath())) {
            throw new IllegalArgumentException("Both element must share the same path");
        }
        // DEV NOTE :: indexed by `SnapshotElement#getKey()`, the same key the snapshot comparison uses. Indexing by
        //             path alone would let a metadata occurrence and a bitstream sharing a path silently overwrite
        //             each other here, while the comparison had correctly treated them as two distinct elements.
        Pair<SnapshotElement, SnapshotElement> change = Pair.of(original, revised);
        this.changes.put(elementOf(change).getKey(), change);
    }
    public List<Pair<SnapshotElement, SnapshotElement>> getChanges() {
        return new ArrayList<>(changes.values());
    }
    public Pair<SnapshotElement, SnapshotElement> getChange(String path) {
        return this.changes.values().stream()
            .filter(change -> Objects.equals(elementOf(change).getPath(), path))
            .findFirst()
            .orElse(null);
    }

}
