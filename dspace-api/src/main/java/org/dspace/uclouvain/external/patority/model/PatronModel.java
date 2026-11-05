/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.patority.model;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.apache.commons.lang3.StringUtils;

/**
 * Main data model for a patority search call.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PatronModel {

    @JsonProperty("fgs_ident")
    private String fgsIdentifier = "";
    @JsonProperty("noma_ident")
    private String nomaIdentifier = "";
    @JsonProperty("last_name")
    private String lastName = "";
    @JsonProperty("first_name")
    private String firstName = "";
    @JsonProperty("alternative_name")
    private String[] alternativeNames = {};
    @JsonProperty("emails_mindent")
    private String[] emails = {};
    @JsonProperty("idm_grids")
    private int[] idmGrids = {};
    @JsonProperty("affiliations")
    private String[] affiliations  = {};
    @JsonProperty("institution")
    private String institution  = "";
    @JsonProperty("orcid_ident")
    private String orcidIdentifier = "";

    /**
     * Generate a detail string for debug purposes.
     */
    @Override
    public String toString() {
        String[] elements = {
            fgsIdentifier,
            nomaIdentifier,
            lastName,
            firstName,
            "< " + Arrays.asList(alternativeNames).stream().collect(Collectors.joining(", ")) + " >",
            "{ " + Arrays.asList(emails).stream().collect(Collectors.joining(", ")) + " }",
            "[ " + Arrays.stream(idmGrids).mapToObj(String::valueOf).collect(Collectors.joining(", ")) + " ]",
            "| " + Arrays.asList(affiliations).stream().collect(Collectors.joining(", ")) + " |",
            institution
        };
        return String.join(" ", elements);
    }

    /** 
     * Retrieves the full name of the person.
     *
     * @return The string that will be displayed
     */
    public String getFullName() {
        return Arrays.stream(alternativeNames)
            .filter(StringUtils::isNotBlank)
            .findFirst()
            .orElse(firstName.isEmpty() ? lastName : lastName + ", " + firstName);
    }

    // SETTERS AND GETTERS
    // Each setter is annotated with @JsonSetter(nulls = Nulls.AS_EMPTY): JSON null values are converted to
    // empty strings / arrays, so that the fields are never null.

    public String getFgsIdentifier() {
        return fgsIdentifier;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setFgsIdentifier(String fgsIdentifier) {
        this.fgsIdentifier = fgsIdentifier;
    }

    public String getNomaIdentifier() {
        return nomaIdentifier;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setNomaIdentifier(String nomaIdentifier) {
        this.nomaIdentifier = nomaIdentifier;
    }

    public String getLastName() {
        return lastName;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String[] getAlternativeNames() {
        return alternativeNames;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setAlternativeNames(String[] alternativeNames) {
        this.alternativeNames = alternativeNames;
    }

    public String[] getEmails() {
        return emails;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setEmails(String[] emails) {
        this.emails = emails;
    }

    public int[] getIdmGrids() {
        return idmGrids;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setIdmGrids(int[] idmGrids) {
        this.idmGrids = idmGrids;
    }

    public String[] getAffiliations() {
        return affiliations;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setAffiliations(String[] affiliations) {
        this.affiliations = affiliations;
    }

    public String getInstitution() {
        return institution;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public String getOrcidIdentifier() {
        return orcidIdentifier;
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setOrcidIdentifier(String orcidIdentifier) {
        this.orcidIdentifier = orcidIdentifier;
    }
}
