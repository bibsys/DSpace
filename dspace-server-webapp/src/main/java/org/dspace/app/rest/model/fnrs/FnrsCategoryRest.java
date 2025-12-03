/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.fnrs;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FnrsCategoryRest extends FnrsExplanationRest {

    public static final String TYPE = "category";

    private boolean isApplicable;
    private final List<FnrsRuleRest> rules = new ArrayList<>();

    // GETTER & SETTER =================================================================================================
    public String getType() {
        return TYPE;
    }

    @JsonProperty("applicable")
    public boolean isApplicable() {
        return isApplicable;
    }
    public void setApplicable(boolean isApplicable) {
        this.isApplicable = isApplicable;
    }

    public List<FnrsRuleRest> getRules() {
        return rules;
    }
    public void addRule(FnrsRuleRest rule) {
        this.rules.add(rule);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Boolean isValid() {
        return (this.isApplicable)
            ? rules.stream().allMatch(FnrsRuleRest::isValid)
            : null;
    }
    public void setValid(boolean isValid) {
        // do nothing: `setValid` must not be called directly.
        // A category is valid only if it's applicable AND all rules are valid.
    }
}
