/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot.diff.explainer;

import java.util.Objects;

import org.dspace.uclouvain.content.snapshot.diff.ItemSnapshotDiff;
import org.dspace.uclouvain.content.snapshot.element.SnapshotElement;
import org.springframework.lang.Nullable;

/**
 * Abstract class used as a super class to explain a change between two version of a {@link SnapshotElement}
 * To be valid snapshot elements must
 *    * either `original` is null, but not `revised`: this is an ADD
 *    * either `revised` is null, but not `original`: this is a REMOVE
 *    * both elements aren't null, share the same path and are not equal: this is an UPDATE
 *
 * @param <T> the concrete {@link SnapshotElement}` classes to work with
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract class DiffExplainer<T extends SnapshotElement> {

    // CLASS ATTRIBUTES ================================================================================================
    private final T original;
    private final T revised;

    // CONSTRUCTOR =====================================================================================================
    public DiffExplainer(@Nullable T original, @Nullable T revised) throws IllegalArgumentException {
        if (original == null && revised == null) {
            throw new IllegalArgumentException("both SnapshotElement cannot be null");
        }
        if (original != null && revised != null) {
            if (!Objects.equals(original.getClass(), revised.getClass())) {
                throw new IllegalArgumentException("both SnapshotElement must share same class");
            }
            if (!Objects.equals(original.getPath(), revised.getPath())) {
                throw new IllegalArgumentException("both SnapshotElement must share same path");
            }
            if (Objects.equals(original, revised)) {
                throw new IllegalArgumentException("both SnapshotElement are logically equal");
            }
        }
        this.original = original;
        this.revised = revised;
    }

    // CLASS METHODS ===================================================================================================

    /** Get the operation type of the change */
    public String getType() {
        if (original == null && revised != null) {
            return ItemSnapshotDiff.ADD;
        } else if (original != null && revised == null) {
            return ItemSnapshotDiff.REMOVE;
        } else {
            return ItemSnapshotDiff.UPDATE;
        }
    }

    /** Get the element path concerned by this change */
    public String getPath() {
        String type = getType();
        return Objects.equals(type, ItemSnapshotDiff.UPDATE) || Objects.equals(type, ItemSnapshotDiff.REMOVE)
            ? original.getPath()
            : revised.getPath();
    }

    // GETTER ==========================================================================================================
    public T getOriginal() {
        return original;
    }
    public T getRevised() {
        return revised;
    }
}