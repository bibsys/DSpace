/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.validation.fnrs;

import java.util.List;

import org.dspace.content.Item;

/**
 * This validator class allows to check if an `Item` validate FNRS rules.
 * This validator should only use with `Publication` DSpace entity type.
 * All rules are defined into the configuration file (config/spring/uclouvain/fnrs.xml)
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class FNRSValidator {

    // CLASS ATTRIBUTES ============================================================================================ -->
    private List<Category> categories;

    // PUBLIC METHODS ============================================================================================== -->

    /**
     * Determine if an item is relevant for FNRS check.
     * To be relevant, the item must be applicable for (at least) one FNRS category (cfr. configuration file)
     * @param item the item to check
     * @return true if the item is relevant, false otherwise
     */
    public boolean isRelevant(Item item) {
        return categories.stream().anyMatch(category -> category.isApplicable(item));
    }

    /**
     * Determine if an item is valid regarding FNRS rules.
     * To be valid, the item must match (at least) one category, and all category rules are valid.
     * @param item the item to validate
     * @return true if the item is valid, false otherwise
     */
    public boolean isValid(Item item) {
        return categories.stream().anyMatch(category -> category.isValid(item));
    }

    // GETTER & SETTERS ============================================================================================ -->
    public List<Category> getCategories() {
        return categories;
    }
    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }
}
