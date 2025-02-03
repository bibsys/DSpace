/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery.indexing;

import org.dspace.content.Item;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexablePoolTask;

public abstract class SolrServiceUCLouvainIndexingPlugin {

    /**
     * Get an instance of the item based on the handled types.
     * @param dso The object to get the item from.
     * @return The item instance or null if the object is not an instance of the handled types.
     */
    protected Item getItem(IndexableObject dso) {
        if (dso instanceof IndexablePoolTask) {
            return ((IndexablePoolTask) dso).getIndexedObject().getWorkflowItem().getItem();
        } else if (dso instanceof IndexableClaimedTask) {
            return ((IndexableClaimedTask) dso).getIndexedObject().getWorkflowItem().getItem();
        } else if (dso instanceof IndexableItem) {
            return ((IndexableItem) dso).getIndexedObject();
        }
        return null;
    }
}