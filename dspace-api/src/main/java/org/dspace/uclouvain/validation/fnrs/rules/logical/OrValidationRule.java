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
 * Validation rule to evaluate if at least one rules is valid
 *   To be valid, rules to evaluate cannot be an empty list. In this specific (and dummy !) case, the validation will
 *   return 'false' as response.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class OrValidationRule extends LogicalValidationRule {

    // OVERRIDE METHODS ================================================================================================
    @Override
    public boolean validate(Item item) {
        // DEV NOTE:: `anyMatch` on an empty stream return 'false'
        return rules.stream().anyMatch(rule -> rule.validate(item));
    }
}
