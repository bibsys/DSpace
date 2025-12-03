/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.validation.fnrs;

import java.util.Collections;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.uclouvain.validation.fnrs.rules.ValidationRule;

public class Category {

    public String name;
    public String description;
    public List<ValidationRule> rules = Collections.emptyList();
    public List<ValidationRule> applicableFor = Collections.emptyList();

    public boolean isApplicable(Item item) {
        return applicableFor.isEmpty() || applicableFor.stream().allMatch(r -> r.validate(item));
    }

    public boolean isValid(Item item) {
        return isApplicable(item) && (rules.isEmpty() || rules.stream().allMatch(r -> r.validate(item)));
    }

    public String toString() {
        return String.format("<%s#%s>", this.getClass().getName(), this.name);
    }

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

    public List<ValidationRule> getRules() {
        return rules;
    }
    public void setRules(List<ValidationRule> rules) {
        this.rules = rules;
    }
    public void setRules(ValidationRule rule) {
        this.rules = Collections.singletonList(rule);
    }

    public List<ValidationRule> getApplicableFor() {
        return applicableFor;
    }
    public void setApplicableFor(List<ValidationRule> rules) {
        this.applicableFor = rules;
    }
    public void setApplicableFor(ValidationRule rule) {
        this.applicableFor = Collections.singletonList(rule);
    }
}
