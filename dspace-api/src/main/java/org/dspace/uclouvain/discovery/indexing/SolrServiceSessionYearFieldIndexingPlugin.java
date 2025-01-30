/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery.indexing;

import static org.dspace.discovery.SearchUtils.FILTER_SEPARATOR;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.MetadataField;

/**
 * This class is a Solr indexing plugin that indexes the session and year fields as one field in Solr.
 */
public class SolrServiceSessionYearFieldIndexingPlugin
        extends SolrServiceUCLouvainIndexingPlugin
        implements SolrServiceIndexPlugin {

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final MetadataField sessionField = new MetadataField(configService
            .getProperty("uclouvain.global.metadata.session.field", "masterthesis.session"));
    private final MetadataField yearField = new MetadataField(configService
            .getProperty("uclouvain.global.metadata.year.field", "dc.date.issued"));;

    @Override
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        Item item = getItem(dso);
        if (item == null) {
            return;
        }
        ItemService itemService = item.getItemService();
        String sessionValue = itemService.getMetadataFirstValue(item, this.sessionField, null);
        String yearValue = itemService.getMetadataFirstValue(item, this.yearField, null);
        if (sessionValue != null && yearValue != null && !sessionValue.isBlank() && !yearValue.isBlank()) {
            String value = sessionValue + " " + yearValue;
            String separator = configService.getProperty("discovery.solr.facets.split.char", FILTER_SEPARATOR);
            document.addField("sessionyear_filter", value.toLowerCase() + separator + value);
            document.addField("sessionyear_keyword", value);
        }
    }
}
