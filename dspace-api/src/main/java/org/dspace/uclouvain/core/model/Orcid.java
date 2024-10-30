/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model;

/** Class that represent an Orcid ID */
public class Orcid {

    private String orcidLink;

    public Orcid(String orcidLink) {
        this.orcidLink = orcidLink;
    }

    /** 
    * Extract the id from an Orcid link
    * 
    * @return The id contained in the Orcid full link
    */
    public String getID() {
        return orcidLink.replace("https://orcid.org/", "");
    }

    public String getOrcid() {
        return orcidLink;
    }

    public void setOrcid(String orcid) {
        this.orcidLink = orcid;
    }
}
