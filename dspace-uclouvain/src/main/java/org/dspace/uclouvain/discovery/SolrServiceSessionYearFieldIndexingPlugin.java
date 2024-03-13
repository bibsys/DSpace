package org.dspace.uclouvain.discovery;

import static org.dspace.discovery.SearchUtils.FILTER_SEPARATOR;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.indexobject.IndexableClaimedTask;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.discovery.indexobject.IndexablePoolTask;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is a Solr indexing plugin that indexes the session and year fields as one field in Solr.
 */
public class SolrServiceSessionYearFieldIndexingPlugin implements SolrServiceIndexPlugin {

    @Autowired
    private ConfigurationService configurationService;

    @Override
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document){
        Item item = this.getItem(dso);
        if (item != null) {
            this.generateSolrSessionYearField(item, document);
        }
    };

    /**
     * Get an instance of the item based on the handled types.
     * @param dso: The object to get the item from.
     * @return: The item instance or null if the object is not an instance of the handled types.
     */
    private Item getItem(IndexableObject dso) {
        if (dso instanceof IndexablePoolTask) {
            return ((IndexablePoolTask) dso).getIndexedObject().getWorkflowItem().getItem();
        } else if (dso instanceof IndexableClaimedTask){
            return ((IndexableClaimedTask) dso).getIndexedObject().getWorkflowItem().getItem();
        } else if (dso instanceof IndexableItem){
            return ((IndexableItem) dso).getIndexedObject();
        }
        return null;
     }

    /**
     * Index the session and year fields as one in Solr.
     * Here we need to index with the '_filter' & '_keyword' suffixes to allow for filtering and faceting.
     * 
     * @param item: The item to index.
     * @param document: The Solr document to index.
     */
    private void generateSolrSessionYearField(Item item, SolrInputDocument document){
        ItemService currentItemService = item.getItemService();
        // TODO: Abstract this to configuration
        String sessionValue = currentItemService.getMetadataFirstValue(item, "masterthesis", "session", null, null);
        String yearValue = currentItemService.getMetadataFirstValue(item, "dc", "date", "issued", null);
        
        if (sessionValue != null && yearValue != null && !sessionValue.isEmpty() && !yearValue.isEmpty()){
            String value = sessionValue + " " + yearValue;
            String valueLowerCase = value.toLowerCase();

            String separator = configurationService.getProperty("discovery.solr.facets.split.char", FILTER_SEPARATOR);

            document.addField("sessionyear_filter", valueLowerCase + separator + value);
            document.addField("sessionyear_keyword", value);
        }
    }
}
