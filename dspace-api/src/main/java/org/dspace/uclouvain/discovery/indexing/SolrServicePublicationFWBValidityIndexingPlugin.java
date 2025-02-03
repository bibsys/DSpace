/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery.indexing;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.UCLouvainFWBValidationService;

/**
 * Solr indexer for FWB decree eligibility and compliance.
 * Stores the 2 keys 'fwbEligible_b' and 'fwbCompliant_b' in the item document.
 * See {@link UCLouvainFWBValidationService} for full FWB rules check.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class SolrServicePublicationFWBValidityIndexingPlugin
    extends SolrServiceUCLouvainIndexingPlugin implements SolrServiceIndexPlugin {

    private final UCLouvainFWBValidationService uclouvainFWBValidationService =
        UCLouvainServiceFactory.getInstance().getFWBValidationService();

    /**
     * Index 2 keys in the item document:
     * - 'fwbEligible_b': Is the item eligible based on FWB requirements.
     * - 'fwbCompliant_b': Is the item eligible based on FWB requirements.
     * 
     * @param context The current DSpace context.
     * @param dso The DSpace Item to process.
     * @param document The Solr document to add the keys to.
     */
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        Item item = getItem(dso);
        if (item == null) {
            return;
        }

        // Get the FWB state of the item.
        Boolean isFwbEligible = uclouvainFWBValidationService.isFWBEligible(context, item);
        Boolean isfFwbCompliant = isFwbEligible
            ? uclouvainFWBValidationService.isFWBCompliantAsBoolean(context, item)
            : false;

        // Add keys and values to document.
        document.addField("fwbEligible_b", isFwbEligible);
        document.addField("fwbCompliant_b", isfFwbCompliant);
    }
}
