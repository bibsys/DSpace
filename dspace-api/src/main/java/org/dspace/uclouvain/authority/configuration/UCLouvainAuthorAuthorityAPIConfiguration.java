/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority.configuration;

public class UCLouvainAuthorAuthorityAPIConfiguration {
    private String filter;
    private String authorityName;

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilter() {
        return this.filter;
    }

    public void setAuthorityName(String authorityName) {
        this.authorityName = authorityName;
    }

    public String getAuthorityName() {
        return this.authorityName;
    }
}
