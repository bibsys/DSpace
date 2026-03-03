/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.queryFilters;

import java.text.ParseException;

import org.apache.solr.client.solrj.util.ClientUtils;

/**
 * Generic Solr query filter class based on a specific field
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class GenericFieldSolrQueryFilter implements SolrQueryFilter {

    private final String solrField;

    public GenericFieldSolrQueryFilter(String solrField) {
        this.solrField = solrField;
    }

    /**
     * Parse a query filter based on a generic solr field exact term
     * @param value the value to parse
     * @return a Solr query filter that could be used to filter a solr response.
     * @throws ParseException if any exception occurred during value parsing
     */
    @Override
    public String parse(String value) throws ParseException {
        return "%s:\"%s\"".formatted(solrField, ClientUtils.escapeQueryChars(value));
    }

}
