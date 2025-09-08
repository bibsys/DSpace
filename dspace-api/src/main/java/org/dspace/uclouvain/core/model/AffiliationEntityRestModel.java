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


public class AffiliationEntityRestModel {
    public UUID UUID;
    public String name;
    public String acronym;
    public String type;
    public boolean isSelectable;
    public int weight;
    public UUID parent;
    public List<AffiliationEntityRestModel> children = new ArrayList<AffiliationEntityRestModel>();

    public AffiliationEntityRestModel() {
    }

    public AffiliationEntityRestModel(AffiliationEntityRestModel model) {
        this.UUID = model.UUID;
        this.name = model.name;
        this.acronym = model.acronym;
        this.type = model.type;
        this.isSelectable = model.isSelectable;
        this.weight = model.weight;
        this.parent = model.parent;
        model.children.forEach(modelChild -> this.children.add(new AffiliationEntityRestModel(modelChild)));
    }
}
