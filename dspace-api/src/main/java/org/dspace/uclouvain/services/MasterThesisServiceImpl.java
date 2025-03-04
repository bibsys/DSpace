/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.tuple.Triple;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of service for specific MasterThesis Item management
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class MasterThesisServiceImpl implements MasterThesisService {

    public static final String MASTER_THESIS_ENTITY_TYPE = "MasterThesis";
    public static final int SOLR_QUERY_MAX_VALUE = 10000;
    // Arbitrary value. Usage of `Integer.MAX_VALUE` causes a problem with SolrCluster

    @Autowired
    SearchService searchService;

    /**
     * Search for MasterThesis item.
     *
     * @param context the application context
     * @param criteria a list of search criteria. Each criterion is a tuple of three values:
     *                 1) search field name
     *                 2) search field value of value part (ex: "CRIM2M" or "CRIM*")
     *                 3) is exact term search
     * @return a list of corresponding Item
     * @throws SearchServiceException for any search exception (solr, database, ...)
     */
    @Override
    public final Iterator<Item> search(Context context, Triple<String, Object, Boolean>... criteria)
            throws SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.addDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.addFilterQueries("search.entitytype:" + MASTER_THESIS_ENTITY_TYPE);
        discoverQuery.addFilterQueries(Arrays.stream(criteria).map(this::buildFilterCriteria).toArray(String[]::new));
        discoverQuery.setMaxResults(SOLR_QUERY_MAX_VALUE);
        discoverQuery.setIncludeNotDiscoverableOrWithdrawn(false);
        return searchService.iteratorSearch(context, null, discoverQuery);
    }


    private String buildFilterCriteria(Triple<String, Object, Boolean> triple) {
        return triple.getRight()
            ? String.format("%s:\"%s\"", triple.getLeft(), triple.getMiddle().toString())
            : String.format("%s:%s", triple.getLeft(), triple.getMiddle().toString());
    }
}
