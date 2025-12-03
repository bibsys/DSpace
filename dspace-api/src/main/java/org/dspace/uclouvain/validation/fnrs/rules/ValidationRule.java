/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.validation.fnrs.rules;

import org.dspace.content.Item;

public abstract class ValidationRule {

    // CLASS ATTRIBUTES ================================================================================================
    public String name;
    public String description;

    // PUBLIC METHODS ==================================================================================================
    public String toString() {
        return String.format("<%s#%s>", this.getClass().getName(), this.name);
    }

    // ABSTRACT METHOD (to override) ===================================================================================
    /**
     * Validate the rule through an `Item`.
     * @param item The item used to validate the rule.
     * @return True if the rule is validated ; false otherwise
     */
    public abstract boolean validate(Item item);

    // GETTER & SETTER =================================================================================================
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

}
