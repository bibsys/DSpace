/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester.actions.configuration;

import java.util.ArrayList;
import java.util.List;

public class ActionField {
    private String field;
    private Integer security;
    private List<ActionField> copyFields = new ArrayList<>();

    // GETTERS AND SETTERS
    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Integer getSecurity() {
        return security;
    }

    public void setSecurity(Integer security) {
        this.security = security;
    }

    public List<ActionField> getCopyFields() {
        return copyFields;
    }

    public void setCopyFields(List<ActionField> copyFields) {
        this.copyFields = copyFields;
    }
}
