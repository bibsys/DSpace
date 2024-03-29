package org.dspace.uclouvain.discovery;

import org.apache.solr.client.solrj.SolrQuery;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SolrServiceSearchPlugin;
import org.dspace.eperson.EPerson;

public class SolrServicePromoterItemPlugin implements SolrServiceSearchPlugin {
    public static final String DISCOVER_PROMOTER_CONFIGURATION_NAME = "promoter";

    @Override
    public void additionalSearchParameters(Context context, DiscoverQuery discoveryQuery, SolrQuery solrQuery) throws SearchServiceException {
        // Check if the current discovery configuration is for the promoter discovery
        boolean isPromoterDiscovery = DISCOVER_PROMOTER_CONFIGURATION_NAME.equals(discoveryQuery.getDiscoveryConfigurationName());

        EPerson currentPerson = context.getCurrentUser();
        if (currentPerson != null && isPromoterDiscovery) {
            // Add a filter to the query that checks for the item field 'advisors.email'
            solrQuery.addFilterQuery("advisors.email:" + currentPerson.getEmail());
        }
    }
}
