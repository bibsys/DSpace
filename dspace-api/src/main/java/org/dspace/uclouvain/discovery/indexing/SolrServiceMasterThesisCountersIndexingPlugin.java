/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery.indexing;

import static org.dspace.uclouvain.core.utils.ItemUtils.extractItemFiles;

import java.util.Map;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.MetadataField;

/** Solr indexing plugin that add some counters for master thesis publications. */
public class SolrServiceMasterThesisCountersIndexingPlugin
        extends SolrServiceUCLouvainIndexingPlugin
        implements SolrServiceIndexPlugin {

    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final Map<String, String> counters = Map.of(
            "authors_counter_i", configService.getProperty("uclouvain.global.metadata.authorname.field"),
            "supervisors_counter_i", configService.getProperty("uclouvain.global.metadata.advisorname.field"),
            "institutions_counter_i", configService.getProperty("uclouvain.global.metadata.authorinstitution.field"),
            "degrees_counter_i", configService.getProperty("uclouvain.global.metadata.degreecode.field"),
            "root_degrees_counter_i", configService.getProperty("uclouvain.global.metadata.rootdegreecode.field"),
            "faculties_counter_i", configService.getProperty("uclouvain.global.metadata.facultycode.field")
    );

    @Override
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        // Check item exists and it's a `MasterThesis` publication
        Item item = getItem(dso);
        if (item == null) {
            return;
        }
        ItemService itemService = item.getItemService();
        String entityType = itemService.getMetadataFirstValue(item, new MetadataField("dspace.entity.type"), null);
        if (!entityType.equals("MasterThesis")) {
            return;
        }
        // Add metadata field counters
        for (Map.Entry<String, String> entry : counters.entrySet()) {
            document.addField(
                    entry.getKey(),
                    itemService.getMetadataByMetadataString(item, entry.getValue()).size()
            );
        }
        // Add bitstream counters
        document.addField("attached_files_counter_i", extractItemFiles(item).size());
    }
}
