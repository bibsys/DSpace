/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.queryFilters;

/**
 * Simple factory to build `sort` parameter to used into a Solr request
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class SolrSortOptionFactory {

    protected SolrSortOptionFactory() {
        // do nothing ... to prevent checkstyle error
    }

    public static SolrSortOption build(String sortOption) throws IllegalArgumentException {
        return switch (sortOption) {
            case "documentType" -> new DocumentTypeSolrQueryFilter();
            case "year" -> new DateRangeSolrQueryFilter();
            default -> throw new IllegalArgumentException("Unknown sort option: " + sortOption);
        };
    }
}
