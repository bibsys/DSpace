/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.utils.DiscoverQueryBuilder;
import org.dspace.discovery.utils.parameter.QueryBuilderSearchFilter;

/**
 * {@link DiscoverQueryBuilder} that makes the user query safe for the Solr standard (Lucene) query parser.
 * <p>
 * The parser treats an unescaped {@code /} as the start of a regular expression, so any query containing a URL
 * or a DOI ({@code 10.1080/xyz}) fails with a syntax error. The query is normalized here, at the single point
 * where user input enters the discovery layer, so the rest of the search pipeline never sees a raw slash:
 * <ul>
 *   <li>a query that is exactly a DOI URL ({@code https://doi.org/10.xxx}) is replaced by the quoted bare DOI,
 *       because the {@code https://doi.org/} prefix is not indexed and {@code https:} would otherwise be parsed
 *       as a field name;</li>
 *   <li>any other query gets its slashes escaped. Only the slash is escaped: the frontend relies on field syntax
 *       ({@code search.resourceid:...}), phrases and wildcards in the same parameter.</li>
 * </ul>
 */
public class UCLouvainDiscoverQueryBuilder extends DiscoverQueryBuilder {

    private static final Pattern DOI_URL =
        Pattern.compile("^(?:https?://)?(?:dx\\.)?doi\\.org/(10\\.\\S+)$", Pattern.CASE_INSENSITIVE);

    @Override
    public DiscoverQuery buildQuery(Context context, IndexableObject scope,
                                    DiscoveryConfiguration discoveryConfiguration,
                                    String query, List<QueryBuilderSearchFilter> searchFilters,
                                    List<String> dsoTypes, Integer pageSize, Long offset, String sortProperty,
                                    String sortDirection)
            throws IllegalArgumentException, SearchServiceException {
        return super.buildQuery(context, scope, discoveryConfiguration, normalizeQuery(query), searchFilters,
                                dsoTypes, pageSize, offset, sortProperty, sortDirection);
    }

    @Override
    public DiscoverQuery buildFacetQuery(Context context, IndexableObject scope,
                                         DiscoveryConfiguration discoveryConfiguration,
                                         String prefix, String query, List<QueryBuilderSearchFilter> searchFilters,
                                         List<String> dsoTypes, Integer pageSize, Long offset, String facetName)
            throws IllegalArgumentException {
        return super.buildFacetQuery(context, scope, discoveryConfiguration, prefix, normalizeQuery(query),
                                     searchFilters, dsoTypes, pageSize, offset, facetName);
    }

    /**
     * Make a user query parseable by the Solr standard query parser (see class documentation).
     *
     * @param query the raw user query, may be null or blank
     * @return the normalized query, or the input unchanged when blank
     */
    static String normalizeQuery(String query) {
        if (StringUtils.isBlank(query)) {
            return query;
        }

        String trimmed = query.trim();
        // DOI MANAGEMENT
        //   if user search using full DOI URL, extract the DOI value and search on it
        Matcher doiMatcher = DOI_URL.matcher(trimmed);
        if (doiMatcher.matches()) {
            return "\"" + doiMatcher.group(1) + "\"";
        }

        // Otherwise, escape forward slashes for safety in queries
        return trimmed.replace("/", "\\/");
    }
}
