/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.patority.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response of a Patority search, it contains one page of patrons data, the total number of matching patrons,
 * an offset representing the current start position for pagination and the limit of element per page.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatoritySearchResult {

    @JsonProperty("total")
    private int total = 0;
    @JsonProperty("offset")
    private int offset = 0;
    @JsonProperty("limit")
    private int limit = 0;
    @JsonProperty("results")
    private PatronModel[] results = {};

    /**
     * @return an empty result (no patron, total of 0), used when the search fails.
     */
    public static PatoritySearchResult empty() {
        return new PatoritySearchResult();
    }

    /**
     * @return the total number of patrons matching the search (all pages included).
     */
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    /**
     * @return the index of the first returned patron (offset of the page).
     */
    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     * @return the maximum number of patrons per page.
     */
    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     * @return the patrons of the requested page (never null).
     */
    public PatronModel[] getResults() {
        return results;
    }

    public void setResults(PatronModel[] results) {
        this.results = (results == null) ? new PatronModel[0] : results;
    }
}