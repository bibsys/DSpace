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
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;
import org.dspace.app.rest.model.RestAddressableModel;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class FnrsRest extends RestAddressableModel {

    // CLASS CONSTANTS =================================================================================================
    public static final String PLURAL_NAME = "fnrs";
    public static final String NAME = "fnrs";
    public static final String CATEGORY = RestAddressableModel.UCLOUVAIN;

    // CLASS ATTRIBUTES ================================================================================================
    private UUID uuid;
    private boolean isRelevant;
    private boolean isValid;
    private final List<FnrsCategoryRest> explanations = new ArrayList<>();

    // ABSTRACT IMPLEMENTED METHODS ====================================================================================
    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    // GETTER & SETTER =================================================================================================
    public String getUuid() {
        return uuid.toString();
    }
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    @JsonProperty("relevant")
    public boolean isRelevant() {
        return isRelevant;
    }
    public void setRelevant(boolean relevant) {
        this.isRelevant = relevant;
    }

    @JsonProperty("valid")
    public boolean isValid() {
        return isValid;
    }
    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    public List<FnrsCategoryRest> getExplanations() {
        return explanations;
    }
    public void addExplanation(FnrsCategoryRest explanation) {
        this.explanations.add(explanation);
    }
}
