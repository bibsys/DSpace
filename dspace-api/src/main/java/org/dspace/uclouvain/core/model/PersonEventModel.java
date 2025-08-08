
/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model;

import java.util.Arrays;
import java.util.List;

/** 
 * Model representing an event on a person.
 * @param fgs The id of the person.
 * @param action The action that has been performed.
 * @param information Additional information for this event.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class PersonEventModel {
    private String fgs;
    private String action;
    private String information;

    public static final String ACTION_CREATE = "create";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_DELETE = "delete";
    public static final List<String> AVAILABLE_ACTIONS = Arrays.asList(
        ACTION_CREATE,
        ACTION_UPDATE,
        ACTION_DELETE
    );

    @Override
    public String toString() {
        return String.format("{fgs: %s, action: %s, information: %s}", fgs, action, information);
    }

    // GETTERS && SETTERS
    public String getFgs() {
        return this.fgs;
    }

    public void setFgs(String fgs) {
        this.fgs = fgs;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getInformation() {
        return information;
    }

    public void setInformation(String information) {
        this.information = information;
    }
}
