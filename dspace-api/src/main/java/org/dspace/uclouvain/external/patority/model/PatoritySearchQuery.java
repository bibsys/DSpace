/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.patority.model;

import java.net.URISyntaxException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

/**
 * Class to hold criteria of a search on the Patority API.
 * Each instance is immutable and created through {@link #builder(String)} class.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public final class PatoritySearchQuery {

    private final String term;
    private final int[] idmGrids;
    private final int offset;
    private final int limit;
    private final String sortField;
    private final boolean highlight;

    private PatoritySearchQuery(Builder builder) {
        this.term = StringUtils.trimToEmpty(builder.term);
        this.idmGrids = builder.idmGrids == null ? new int[0] : Arrays.stream(builder.idmGrids).distinct().toArray();
        this.offset = Math.max(0, builder.offset);
        this.limit = Math.max(1, builder.limit);
        this.sortField = StringUtils.trimToNull(builder.sortField);
        this.highlight = builder.highlight;
    }

    /**
     * @param term The search term (first and/or last name)
     * @return A builder for a search on the given term
     */
    public static Builder builder(String term) {
        return new Builder(term);
    }

    /**
     * @return True if the query has a non-blank search term
     */
    public boolean hasTerm() {
        return !term.isEmpty();
    }

    /**
     * Build the (URL-encoded) URL of this search, relative to the Patority base URL.
     *
     * @param endpoint The search endpoint (e.g. "/search")
     * @return The URL including the query string
     * @throws URISyntaxException if the URL cannot be built
     */
    public String toUrl(String endpoint) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(endpoint);
        if (hasTerm()) {
            uriBuilder.addParameter("term", term);
        }
        for (int idmGrid : idmGrids) {
            uriBuilder.addParameter("idm_grids", String.valueOf(idmGrid));
        }
        if (sortField != null) {
            uriBuilder.addParameter("sort_field", sortField);
        }
        uriBuilder.addParameter("offset", String.valueOf(offset));
        uriBuilder.addParameter("limit", String.valueOf(limit));
        uriBuilder.addParameter("highlight", String.valueOf(highlight));
        return uriBuilder.build().toString();
    }

    @Override
    public String toString() {
        return "PatoritySearchQuery{term='" + term + "', idmGrids=" + Arrays.toString(idmGrids)
            + ", offset=" + offset + ", limit=" + limit + ", sortField=" + sortField
            + ", highlight=" + highlight + "}";
    }

    // GETTERS

    public String getTerm() {
        return term;
    }

    public int[] getIdmGrids() {
        return idmGrids.clone();
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public String getSortField() {
        return sortField;
    }

    public boolean isHighlight() {
        return highlight;
    }

    /**
     * Builder of {@link PatoritySearchQuery}. Defaults: no IDM grid, offset 0, limit 20, no sort, no highlight.
     */
    public static final class Builder {

        private static final int DEFAULT_LIMIT = 20;

        private final String term;
        private int[] idmGrids;
        private int offset = 0;
        private int limit = DEFAULT_LIMIT;
        private String sortField;
        private boolean highlight = false;

        private Builder(String term) {
            this.term = term;
        }

        /**
         * @param idmGrids The IDM grids used to filter the patrons (duplicates are ignored)
         * @return This builder
         */
        public Builder idmGrids(int... idmGrids) {
            this.idmGrids = idmGrids == null ? null : idmGrids.clone();
            return this;
        }

        /**
         * @param offset Index of the first result (negative values are replaced by 0)
         * @return This builder
         */
        public Builder offset(int offset) {
            this.offset = offset;
            return this;
        }

        /**
         * @param limit Maximum number of results (values lower than 1 are replaced by 1)
         * @return This builder
         */
        public Builder limit(int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * @param sortField The field used to sort results (any sort field known by Patority; blank means no sort)
         * @return This builder
         */
        public Builder sortField(String sortField) {
            this.sortField = sortField;
            return this;
        }

        /**
         * @param highlight Whether the API should highlight matching terms
         * @return This builder
         */
        public Builder highlight(boolean highlight) {
            this.highlight = highlight;
            return this;
        }

        public PatoritySearchQuery build() {
            return new PatoritySearchQuery(this);
        }
    }
}