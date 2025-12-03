/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.validation.fnrs.rules.logical;

import java.util.Collections;
import java.util.List;

import org.dspace.uclouvain.validation.fnrs.rules.ValidationRule;

/**
 * Abstract validation rule to evaluate logical expression
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract class LogicalValidationRule extends ValidationRule {

    // CLASS ATTRIBUTES ================================================================================================
    protected List<ValidationRule> rules = Collections.emptyList();

    // GETTER & SETTER =================================================================================================
    public List<ValidationRule> getRules() {
        return rules;
    }
    public void setRules(List<ValidationRule> rules) {
        this.rules = rules;
    }
}
