/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.tracking;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Simple class used to store any changes operated between 2 versions of an object
 * @param <T> could be any object; should be a {@link org.dspace.content.DSpaceObject} in the Dspace project
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class Changes<T> {

    List<T> added = new ArrayList<>();
    List<T> removed = new ArrayList<>();
    List<Pair<T, T>> updated = new ArrayList<>();

    // METHODS =========================================================================================================
    public boolean isEmpty() {
        return added.isEmpty() && removed.isEmpty() && updated.isEmpty();
    }
    public int size() {
        return added.size() + removed.size() + updated.size();
    }

    // GETTER & SETTER =================================================================================================
    public List<T> added() {
        return added;
    }
    public void detectAdd(T snapshot) {
        added.add(snapshot);
    }

    public List<T> removed() {
        return removed;
    }
    public void detectRemove(T snapshot) {
        removed.add(snapshot);
    }

    public List<Pair<T, T>> updated() {
        return updated;
    }
    public void detectUpdate(T oldSnapshot, T newSnapshot) {
        updated.add(Pair.of(oldSnapshot, newSnapshot));
    }
}
