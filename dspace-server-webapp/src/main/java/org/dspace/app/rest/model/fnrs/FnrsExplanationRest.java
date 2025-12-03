/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.fnrs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public abstract class FnrsExplanationRest {

    protected String name;
    protected String type;
    protected String description;
    private boolean isValid;

    // GETTER & SETTER =================================================================================================
    public abstract String getType();

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("valid")
    public Boolean isValid() {
        return isValid;
    }
    public void setValid(boolean valid) {
        this.isValid = valid;
    }
}
