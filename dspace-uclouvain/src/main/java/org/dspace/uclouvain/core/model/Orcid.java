package org.dspace.uclouvain.core.model;

/** 
* Class that represent an Orcid
*/
public class Orcid {

    private String orcidLink;

    public Orcid(String orcidLink) {
        this.orcidLink = orcidLink;
    }

    /** 
    * Extract the id from the orcid link
    * 
    * @return: The id contained in the orcid full link
    */
    public String getID(){
        return orcidLink.replace("https://orcid.org/", "");
    }

    public String getOrcid(){
        return orcidLink;
    }

    public void setOrcid(String orcid){
        this.orcidLink = orcid;
    }
}
