/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.search.services;

import java.util.List;

import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.uclouvain.search.model.SolrSearchResponse;

public interface UCLouvainSearchService {
    /**
     * Perform a SOLR search on all available publications.
     * @param context The current DSpace application context.
     * @param query The query to execute.
     * @param filterQueries The filters queries.
     * @param page The page number of the result.
     * @param size The size of each page.
     * @return A SolrSearchResponse object containing the search result and the search parameters.
     * @throws SearchServiceException if any exception occurres while querying Solr.
     */
    public SolrSearchResponse searchPublications(
        Context context, String query, List<String> filterQueries, int page, int size
    ) throws SearchServiceException;
}
