package org.dspace.uclouvain.dilbert.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DialPerson {

    @JsonProperty("alternatename")
    public String alternateName = "";
    @JsonProperty("firstname")
    public String firstname = "";
    @JsonProperty("id_patron")
    public String idPatron = "";
    @JsonProperty("lastname")
    public String lastname = "";
    @JsonProperty("orcid_id")
    public String orcidId = "";
    public DialPersonInscription inscription;

    /** 
     * Joins different attributes in one string for display purposes
     * 
     * @return: The string that will be displayed
     */
    public String generateString(){
        String[] elements = {this.alternateName, this.firstname, this.idPatron, this.lastname};
        return String.join(" ", elements);
    }

    /** 
     * Retrieves the full name of the person.
     *
     * 
     * @return: The string that will be displayed
     */
    public String getFullName(){
        if (!this.alternateName.isEmpty()) {
            return this.alternateName;
        }
        return this.firstname.isEmpty()
            ? this.lastname
            : this.lastname + ", " + this.firstname;
    }

    // Setters and getters 

    public String getEntity(){
        return inscription.entity;
    }

    public String getInstitution(){
        return inscription.institution;
    }

    public String getUniqueId(){
        return inscription.primary_id;
    }

    public String getEmail(){
        return inscription.email;
    }

    public String getPrimaryId(){
        return inscription.primary_id;
    }

    public String getEmployeeId(){
        return inscription.employee_id;
    }

    public String getAlternateName() {
        return this.alternateName;
    }

    public void setAlternateName(String alternateName) {
        this.alternateName = alternateName;
    }

    public String getFirstname() {
        return this.firstname;
    }

    public void setFirstname(String firstName) {
        this.firstname = firstName;
    }

    public String getId_patron() {
        return this.idPatron;
    }

    public void setId_patron(String idPatron) {
        this.idPatron = idPatron;
    }

    public String getLastName() {
        return this.lastname;
    }

    public void setLastName(String lastName) {
        this.lastname = lastName;
    }

    public String getOrcidId() {
        return this.orcidId;
    }

    public void setOrcidId(String orcidId) {
        this.orcidId = orcidId;
    }

    public DialPersonInscription getInscription() {
        return this.inscription;
    }

    public void setInscription(DialPersonInscription inscription) {
        this.inscription = inscription;
    }

    // public String getSubData(){
    //     String base = inscription.institution;
    //     if(!inscription.entity.isEmpty()){
    //         base += (" " + inscription.entity);
    //     }
    //     if(!orcid_id.isEmpty()){
    //         base += (" " + orcid_id);
    //     }
    //     return base;
    // }
}
