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
 * Validation rule to evaluate if a set of rules are each valid
 *   if no rules are defined, none can't be invalid, then 'true' is retrurned
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class AndValidationRule extends LogicalValidationRule {

    // OVERRIDE METHODS ================================================================================================
    @Override
    public boolean validate(Item item) {
        return rules.stream().allMatch(rule -> rule.validate(item));
    }
}
