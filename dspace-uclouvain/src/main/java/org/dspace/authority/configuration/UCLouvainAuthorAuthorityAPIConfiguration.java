package org.dspace.authority.configuration;

public class UCLouvainAuthorAuthorityAPIConfiguration {
    private String filter;
    private String authorityName;

    public void setFilter(String filter){
        this.filter = filter;
    }

    public String getFilter(){
        return this.filter;
    }

    public void setAuthorityName(String authorityName){
        this.authorityName = authorityName;
    }

    public String getAuthorityName(){
        return this.authorityName;
    }
}
