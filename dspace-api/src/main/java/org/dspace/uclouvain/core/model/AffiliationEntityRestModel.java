/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;


public class AffiliationEntityRestModel {
    public UUID uuid;
    public String name;
    public String acronym;
    public String type;
    public boolean isSelectable;
    public int weight;
    public UUID parent;
    public List<AffiliationEntityRestModel> children = new ArrayList<>();

    @JsonProperty("documentCount")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Long relatedPublicationCount = null;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String displayAcronym;


    public AffiliationEntityRestModel(OrgUnit model) {
        this.uuid = model.getID();
        this.name = model.getTitle();
        this.acronym = model.getAcronym();
        this.displayAcronym = model.getDisplayAcronym();
        this.type = model.getType();
        this.isSelectable = model.isSelectable();
        this.weight = model.getWeight();
        this.parent = (model.getParent() != null) ? model.getParent().getID() : null;
    }
}