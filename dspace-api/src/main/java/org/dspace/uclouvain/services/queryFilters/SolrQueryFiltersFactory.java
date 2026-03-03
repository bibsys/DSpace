/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.queryFilters;

/**
 * Simple factory to build `fq` query to used into a Solr request
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class SolrQueryFiltersFactory {

    protected SolrQueryFiltersFactory() {
        // do nothing ... to prevent checkstyle error
    }

    public static SolrQueryFilter build(String filterKey) throws IllegalArgumentException {
        return switch (filterKey) {
            case "documentType" -> new DocumentTypeSolrQueryFilter();
            case "year" -> new DateRangeSolrQueryFilter();
            case "includePoster" -> new IncludePosterSolrQueryFilter();
            case "fundingProgram" -> new GenericFieldSolrQueryFilter("fundingProgram_keyword");
            case "entityType" -> new GenericFieldSolrQueryFilter("search.entitytype");
            case "fwbExportable" -> new GenericFieldSolrQueryFilter("fwbExportable_b");
            case "fnrsValid" -> new GenericFieldSolrQueryFilter("fnrsValid_b");
            default -> throw new IllegalArgumentException("Unknown filter key: " + filterKey);
        };
    }
}
