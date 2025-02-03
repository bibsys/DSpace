/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model;

/** 
 * Model to represent a validation regarding FWB rules.
 * A validation object is composed of a validation state 'isValid' and an optional error message 'errorMessage'.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class FWBValidation {
    public boolean isValid;
    public String errorMessage;

    public FWBValidation(boolean isValid, String message) {
        this.isValid = isValid;
        this.errorMessage = message;
    }

    public FWBValidation(boolean isValid) {
        this.isValid = isValid;
    }
}
