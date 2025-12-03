/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.validation.fnrs.rules.logical;

import org.dspace.content.Item;

/**
 * Validation rule to evaluate if at all rules are invalid
 *   If no rules are defined, none rule cannot be valid, then 'true' will be returned
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class NotValidationRule extends LogicalValidationRule {

    // OVERRIDE METHODS ================================================================================================
    @Override
    public boolean validate(Item item) {
        return rules.stream().noneMatch(rule -> rule.validate(item));
    }
}
