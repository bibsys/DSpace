/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery.indexing;

import java.util.Optional;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.uclouvain.core.model.ItemModel;

/**
 * Base class for UCLouvain Solr indexing plugins working on a typed {@link ItemModel}.
 * <p>
 * Every {@link SolrServiceIndexPlugin} is called for every indexed object. This class does the
 * dispatching once: it extracts the DSpace {@link Item} from the indexable object (item, pool task or
 * claimed task), builds the typed model through {@link #buildModel(Item)} and only then calls
 * {@link #additionalIndex(Context, ItemModel, SolrInputDocument)}. Objects that are not items, or items
 * for which no model is built, are silently skipped.
 *
 * @param <T> the model type handled by the plugin.
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract class SolrServiceUCLouvainIndexingPlugin<T extends ItemModel> implements SolrServiceIndexPlugin {

    @Override
    @SuppressWarnings("rawtypes")
    public final void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        Item item = extractItem(dso);
        if (item == null) {
            return;
        }
        buildModel(item).ifPresent(model -> additionalIndex(context, model, document));
    }

    /**
     * Build the typed model handled by this plugin.
     *
     * @param item the DSpace item to wrap.
     * @return the model instance, or empty if this plugin has nothing to index for the item.
     */
    protected abstract Optional<T> buildModel(Item item);

    /**
     * Add plugin-specific fields to the Solr document of the given model.
     *
     * @param context  the current DSpace context.
     * @param model    the typed model built from the indexed item.
     * @param document the Solr document to add the keys to.
     */
    protected abstract void additionalIndex(Context context, T model, SolrInputDocument document);

    /**
     * Get the item wrapped by an indexable object, based on the handled types.
     *
     * @param dso the indexable object to get the item from.
     * @return the item, or null if the object is not an instance of the handled types.
     */
    @SuppressWarnings("rawtypes")
    private static Item extractItem(IndexableObject dso) {
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
