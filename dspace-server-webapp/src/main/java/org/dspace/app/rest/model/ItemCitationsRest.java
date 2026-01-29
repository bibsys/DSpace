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
import org.dspace.uclouvain.citations.CitationEntry;

/**
 * Representation of a list of citation for a specific item.
 * Each citation is defined by a format and a value (the citation itself), see {@link CitationEntry}.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ItemCitationsRest extends BaseObjectRest<UUID> {

    public static final String NAME = "citation";
    public static final String PLURAL_NAME = "citations";
    public static final String CATEGORY = RestAddressableModel.CORE;

    List<CitationEntry> citations = new ArrayList<>();

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

    @Override
    @JsonIgnore
    public Class<?> getController() {
        return RestResourceController.class;
    }

    public List<CitationEntry> getCitations() {
        return this.citations;
    }

    public void setCitations(List<CitationEntry> citations) {
        this.citations = citations;
    }
}
