/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.search.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Object to send as an API response for a search result.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class SolrSearchResponse {
    private long total;
    private int page;
    private int size;
    private List<Map<String, Object>> results = new ArrayList<>();

    // GETTERS && SETTERS

    public long getTotal() {
        return total;
    }

    public SolrSearchResponse setTotal(long total) {
        this.total = total;
        return this;
    }

    public int getPage() {
        return page;
    }

    public SolrSearchResponse setPage(int page) {
        this.page = page;
        return this;
    }

    public int getSize() {
        return size;
    }

    public SolrSearchResponse setSize(int size) {
        this.size = size;
        return this;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public SolrSearchResponse setResults(List<Map<String, Object>> results) {
        this.results = results;
        return this;
    }
}
