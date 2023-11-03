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
    private int codeCategorieDecret;
    private int codeEtatInscription;
    private int cycle;
    private String etatInscription;
    private String faculte;
    private String intitOffreAbrege;
    private String intitOffreComplet;
    private String princSec;
    private String resultatFinal;
    private boolean reussite;
    private String sigleOffreComplet;
    private String sigleOffreCompletN;
    private String sigleOffreRacine;
    private String statut;
    private Optional<String> erreurDossier;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new LinkedHashMap<String, Object>();

    // Utils methods
    
    public boolean isError(){
        return erreurDossier != null ? true: false;
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

    public int getCodeCategorieDecret() {
        return this.codeCategorieDecret;
    }

    public void setCodeCategorieDecret(int codeCategorieDecret) {
        this.codeCategorieDecret = codeCategorieDecret;
    }

    public int getCodeEtatInscription() {
        return this.codeEtatInscription;
    }

    public void setCodeEtatInscription(int codeEtatInscription) {
        this.codeEtatInscription = codeEtatInscription;
    }

    public int getCycle() {
        return this.cycle;
    }

    public void setCycle(int cycle) {
        this.cycle = cycle;
    }

    public String getEtatInscription() {
        return this.etatInscription;
    }

    public void setEtatInscription(String etatInscription) {
        this.etatInscription = etatInscription;
    }

    public String getFaculte() {
        return this.faculte;
    }

    public void setFaculte(String faculte) {
        this.faculte = faculte;
    }

    public String getIntitOffreAbrege() {
        return this.intitOffreAbrege;
    }

    public void setIntitOffreAbrege(String intitOffreAbrege) {
        this.intitOffreAbrege = intitOffreAbrege;
    }

    public String getIntitOffreComplet() {
        return this.intitOffreComplet;
    }

    public void setIntitOffreComplet(String intitOffreComplet) {
        this.intitOffreComplet = intitOffreComplet;
    }

    public String getPrincSec() {
        return this.princSec;
    }

    public void setPrincSec(String princSec) {
        this.princSec = princSec;
    }

    public String getResultatFinal() {
        return this.resultatFinal;
    }

    public void setResultatFinal(String resultatFinal) {
        this.resultatFinal = resultatFinal;
    }

    public boolean isReussite() {
        return this.reussite;
    }

    public void setReussite(boolean reussite) {
        this.reussite = reussite;
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

    public String getSigleOffreRacine(){
        return this.sigleOffreRacine;
    }

    public void setSigleOffreRacine(String sigleOffreRacine){
        this.sigleOffreRacine = sigleOffreRacine;
    }

    public String getStatut() {
        return this.statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
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
