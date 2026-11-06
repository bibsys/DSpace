/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import java.sql.SQLException;
import java.util.Collection;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.dspace.utils.DSpace;

/**
 * Set of util methods for Discovery indexing.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class DiscoveryUtils {

    // Prevent util class instantiation.
    private DiscoveryUtils() {
        throw new UnsupportedOperationException("Utility class: Can't be instanced");
    }

    /**
     * Force the reindexation of a collection of items into Solr.
     * NOTE: All indexable representations of an item are reindexed (archived item, but also workspace/workflow item if
     * the item is still in submission).
     *
     * @param context The current DSpace application context.
     * @param items The collection of items to reindex.
     * @throws SQLException if any database exception occurred.
     * @throws SearchServiceException if the Solr commit failed.
     */
    public static void reindexItems(Context context, Collection<Item> items)
            throws SQLException, SearchServiceException {
        if (items == null || items.isEmpty()) {
            return;
        }
        // DEVNOTE: 'new DSpace()' does not create a new DSpace instance,
        // it only returns a link to the current DSpace kernel.
        IndexingService indexingService = new DSpace().getSingletonService(IndexingService.class);
        IndexObjectFactoryFactory indexObjectFactoryFactory = IndexObjectFactoryFactory.getInstance();
        for (Item item : items) {
            for (IndexableObject indexableObject : indexObjectFactoryFactory.getIndexableObjects(context, item)) {
                // Trigger a re-index of the item. Force true, means the object will always be
                // re-indexed (even if no changes).
                indexingService.indexContent(context, indexableObject, true);
            }
        }
        // Always commit after re-index.
        indexingService.commit();
    }
}
