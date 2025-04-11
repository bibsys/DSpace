/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.RestResourceController;

/**
 * Representation of a list of citation for a specific item.
 * Each citation is defined by a format and a value (the citation itself), see {@link ItemCitationRest}.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ItemCitationsRest extends BaseObjectRest<UUID> {
    public static final String NAME = "citation";
    public static final String PLURAL_NAME = "citations";
    public static final String CATEGORY = RestAddressableModel.CORE;

    List<ItemCitationRest> citations = new ArrayList<>();

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @JsonIgnore
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    public ItemCitationsRest() {
    }

    public List<ItemCitationRest> getCitations() {
        return this.citations;
    }

    public void setCitations(List<ItemCitationRest> citations) {
        this.citations = citations;
    }

    public void addCitation(String format, String citation) {
        this.citations.add(new ItemCitationRest(format, citation));
    }

    @Override
    @JsonIgnore
    public Class<?> getController() {
        return RestResourceController.class;
    }
}
