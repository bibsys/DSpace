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
