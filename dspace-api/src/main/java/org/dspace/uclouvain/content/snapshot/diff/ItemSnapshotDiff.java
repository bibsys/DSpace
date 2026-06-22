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

    // CLASS ATTRIBUTES ================================================================================================
    private final Item item;
    /**
     * Store changes and index them.
     *   If left element exists but not right element --> this is a remove
     *   If left element is null, but right element exists --> this is an add
     *   If both element exists --> this is an update (!!! both element MUST share same path)
     */
    private final Map<String, Pair<SnapshotElement, SnapshotElement>> changes;

    // CLASS CONSTRUCTOR ===============================================================================================
    public ItemSnapshotDiff(Item item) {
        this.item = item;
        this.changes = new HashMap<>();
    }

    // CLASS METHODS ===================================================================================================
    public boolean hasChanges() {
        return !changes.isEmpty();
    }

    // GETTER & SETTER =================================================================================================
    public Item getItem() {
        return item;
    }
    public void addChange(SnapshotElement original, SnapshotElement revised) {
        if (original != null && revised != null && !Objects.equals(original.getPath(), revised.getPath())) {
            throw new IllegalArgumentException("Both element must share the same path");
        }
        String path = (original != null) ? original.getPath() : revised.getPath();
        this.changes.put(path, Pair.of(original, revised));
    }
    public List<Pair<SnapshotElement, SnapshotElement>> getChanges() {
        return new ArrayList<>(changes.values());
    }
    public Pair<SnapshotElement, SnapshotElement> getChange(String path) {
        return this.changes.getOrDefault(path, null);
    }

}
