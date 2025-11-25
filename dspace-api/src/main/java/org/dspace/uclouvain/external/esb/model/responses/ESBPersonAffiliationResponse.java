/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.esb.model.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A model representing affiliations of a person.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ESBPersonAffiliationResponse extends ESBPersonResponse {
    public static final String DEPARTMENT_TYPE_DOCTORAL_SECTOR = "S";
    public static final String DEPARTMENT_TYPE_FACULTY = "F";
    public static final String DEPARTMENT_TYPE_INSTITUTE = "I";
    public static final String DEPARTMENT_TYPE_LOGISTIC = "L";
    public static final String DEPARTMENT_TYPE_DOCTORAL_COMMISSION = "D";
    public static final String DEPARTMENT_TYPE_POLE = "P";
    public static final String DEPARTMENT_TYPE_SCHOOL = "E";
    public static final String DEPARTMENT_TYPE_TECH = "T";

    public static final String LINK_TYPE_AFFECTATION = "Affectation";
    public static final String LINK_TYPE_BELONGING = "Appartenance";

    private String linkType;
    private Entity entity;

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public Entity getEntity() {
        return entity;
    }

    public void setEntity(Entity entity) {
        this.entity = entity;
    }

    public class Entity extends ESBPersonResponse {
        @JsonProperty("entity_id")
        private String entityId;
        private String acronym;
        private String acronyms;
        private String fullAcronym;
        private String departmentType;
        @JsonProperty("name_fr")
        private String nameFr;
        @JsonProperty("name_en")
        private String nameEn;

        public String getEntityId() {
            return entityId;
        }

        public void setEntityId(String entityId) {
            this.entityId = entityId;
        }

        public String getAcronym() {
            return acronym;
        }

        public void setAcronym(String acronym) {
            this.acronym = acronym;
        }

        public String getAcronyms() {
            return acronyms;
        }

        public void setAcronyms(String acronyms) {
            this.acronyms = acronyms;
        }

        public String getFullAcronym() {
            return fullAcronym;
        }

        public void setFullAcronym(String fullAcronym) {
            this.fullAcronym = fullAcronym;
        }

        public String getDepartmentType() {
            return departmentType;
        }

        public void setDepartmentType(String departmentType) {
            this.departmentType = departmentType;
        }

        public String getNameFr() {
            return nameFr;
        }

        public void setNameFr(String nameFr) {
            this.nameFr = nameFr;
        }

        public String getNameEn() {
            return nameEn;
        }

        public void setNameEn(String nameEn) {
            this.nameEn = nameEn;
        }
    }

    @Override
    public String toString() {
        return "{%s[EntityId: %s, acronym: %s, acronyms: %s, fullAcronym: %s, departmentType: %s]}".formatted(
            this.getClass().getName() + "@" + Integer.toHexString(this.hashCode()),
            entity.entityId, entity.acronym, entity.acronyms, entity.fullAcronym, entity.departmentType
        );
    }
}
