/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.queryFilters;

import java.text.ParseException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.discovery.configuration.DiscoverySearchFilterFacet;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Solr query filter class to manage document type filter.
 * It's possible to filter on multiple document type in the same time using a "," character between each allowed value.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class DocumentTypeSolrQueryFilter implements SolrQueryFilter, SolrSortOption {

    private final DiscoverySearchFilterFacet publicationTypeFilter;

    public DocumentTypeSolrQueryFilter() {
        this.publicationTypeFilter = DSpaceServicesFactory
            .getInstance()
            .getServiceManager()
            .getServiceByName("searchFilterPublicationType", DiscoverySearchFilterFacet.class);
    }

    /**
     * Parse a query filter corresponding to a document type filter
     * @param value the value to parse
     * @return a Solr query filter that could be used to filter a solr response.
     * @throws ParseException if any exception occurred during value parsing
     */
    @Override
    public String parse(String value) throws ParseException {
        if (StringUtils.isBlank(value)) {
            throw new ParseException("Empty filters", 0);
        }
        String indexField = publicationTypeFilter.getIndexFieldName();
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .map(v -> "%s:\"%s\"".formatted(indexField, ClientUtils.escapeQueryChars(v)))
            .collect(Collectors.joining(" OR "));
    }

    /** Return the Solr field to use to sort on. */
    @Override
    public String getSortField() {
        return "dc.type.maintype_sort";
    }
}
