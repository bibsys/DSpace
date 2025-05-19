/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.osis.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName("return")
public class OSISStudentDegree {
    private int anac;
    private String categorieDecret;
    private int cycle;
    private String intitOffreComplet;
    private String sigleOffreComplet;
    private String sigleOffreCompletN;
    private String sigleOffreRacine;
    private Optional<String> erreurDossier = Optional.empty();

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<>();

    // Utils methods
    public boolean isError() {
        return erreurDossier.isPresent();
    }

    // Getters and setters
    public int getAnac() {
        return this.anac;
    }
    public void setAnac(int anac) {
        this.anac = anac;
    }
    public String getCategorieDecret() {
        return this.categorieDecret;
    }
    public void setCategorieDecret(String categorieDecret) {
        this.categorieDecret = categorieDecret;
    }
    public int getCycle() {
        return this.cycle;
    }
    public void setCycle(int cycle) {
        this.cycle = cycle;
    }
    public String getIntitOffreComplet() {
        return this.intitOffreComplet;
    }
    public void setIntitOffreComplet(String intitOffreComplet) {
        this.intitOffreComplet = intitOffreComplet;
    }
    public String getSigleOffreComplet() {
        return this.sigleOffreComplet;
    }
    public void setSigleOffreComplet(String sigleOffreComplet) {
        this.sigleOffreComplet = sigleOffreComplet;
    }
    public String getSigleOffreCompletN() {
        return this.sigleOffreCompletN;
    }
    public void setSigleOffreCompletN(String sigleOffreCompletN) {
        this.sigleOffreCompletN = sigleOffreCompletN;
    }
    public String getSigleOffreRacine() {
        return this.sigleOffreRacine;
    }
    public void setSigleOffreRacine(String sigleOffreRacine) {
        this.sigleOffreRacine = sigleOffreRacine;
    }
    public Optional<String> getErreurDossier() {
        return this.erreurDossier;
    }
    public void setErreurDossier(String erreurDossier) {
        this.erreurDossier = Optional.of(erreurDossier);
    }

    // Getter and setter for all other elements
    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }
    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }
}
