/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority.configuration;

import com.google.api.client.util.ArrayMap;

public class UCLouvainAuthorityAPIConfiguration {
    private ArrayMap<String, String> filters;

    public String getFilterByKey(String filter) {
        return filters.get(filter);
    }

    public ArrayMap<String, String> getFilters() {
        return filters;
    }

    public void setFilters(ArrayMap<String, String> filters) {
        this.filters = filters;
    }
}
