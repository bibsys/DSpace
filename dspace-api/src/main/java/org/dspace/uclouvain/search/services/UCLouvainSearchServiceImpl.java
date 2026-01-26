/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.search.services;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.search.model.SolrSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to directly query SOLR without going through DSpace layer.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainSearchServiceImpl implements UCLouvainSearchService {

    @Autowired
    protected SolrSearchCore solrSearchCore;

    private List<String> fieldValidationRegexps = new ArrayList<>();

    private static final Pattern SOLR_ERROR_PATTERN =
        Pattern.compile("org\\.apache\\.solr\\.search\\.(.+?)(?:\\r?\\n|$)");
    private static final String DEFAULT_SOLR_ERROR_MESSAGE = "Search exception occurred";

    public SolrSearchResponse searchPublications(
        Context context, String query, List<String> filterQueries, int page, int size
    ) throws SearchServiceException {
        SolrQuery solrQuery = new SolrQuery(query);
        // Default filters: Keep only Publication items which are archived, discoverable and not withdrawn.
        String[] defaultFilters = {
            "search.resourcetype:Item",
            "dspace.entity.type:" + Publication.ENTITY_TYPE,
            "archived:true",
            "discoverable:true",
            "withdrawn:false"
        };
        solrQuery.addFilterQuery(defaultFilters);
        // Add user's filters.
        Optional.ofNullable(filterQueries).ifPresent(filters -> filters.forEach(solrQuery::addFilterQuery));
        solrQuery.setStart(page * size);
        solrQuery.setRows(size);

        // Query SOLR using created SolrQuery
        try {
            QueryResponse response = solrSearchCore.getSolr().query(solrQuery, solrSearchCore.REQUEST_METHOD);
            SolrDocumentList results = response.getResults();

            SolrSearchResponse searchResponse = new SolrSearchResponse()
                .setPage(page)
                .setSize(size)
                .setTotal(results.getNumFound())
                .setResults(parseDocs(results));

            return searchResponse;
        } catch (Exception e) {
            throw new SearchServiceException(parseSolrMessage(e.getMessage()));
        }
    }

    /**
     * Parse a SolrDocumentList into a usable List of Map.
     * Each document (=publication) is converted into a Map of String/object (field/value).
     * @param docs The SolrDocumentList object.
     * @return A List of Map<String, Object> representing the field/values pair of each publication.
     */
    private List<Map<String, Object>> parseDocs(SolrDocumentList docs) {
        return docs.stream().map(this::parseDoc).toList();
    }

    /**
     * Convert a SolrDocument into a Map of String Object.
     * EAch key of the map is a solr field and each value is the value for that field.
     * @param doc the SolrDocument object to convert.
     * @return A Map containing all the SolrDocument's fields and their corresponding values.
     */
    private Map<String, Object> parseDoc(SolrDocument doc) {
        Map<String, Object> cleaned = new HashMap<>();
        doc.getFieldNames().forEach(field -> {
            if (isValid(field)) {
                cleaned.put(field, doc.getFieldValue(field));
            }
        });
        return cleaned;
    }

    /**
     * A field is valid if at least one of the configured regexp matches.
     * @param field The field to check validity of
     */
    private boolean isValid(String field) {
        return fieldValidationRegexps.stream().anyMatch(regex -> field.matches(regex));
    }

    /**
     * Parse a Solr error to keep only the interesting part of the message (and not leak any sensitive data).
     * @param message The error message.
     * @return A cleaned version of the error message.
     */
    private String parseSolrMessage(String message) {
        if (isEmpty(message)) {
            return DEFAULT_SOLR_ERROR_MESSAGE;
        }
        // Keep only the part of the error message matching this regex.
        Matcher matcher = SOLR_ERROR_PATTERN.matcher(message);
        // Default error message if pattern does not match.
        return matcher.find() ? matcher.group(1).trim() : DEFAULT_SOLR_ERROR_MESSAGE;
    }

    // GETTER && SETTERS

    public void setFieldValidationRegexps(List<String> fieldValidationRegexps) {
        this.fieldValidationRegexps = fieldValidationRegexps;
    }

    public List<String> getFieldValidationRegexps() {
        return fieldValidationRegexps;
    }
}
