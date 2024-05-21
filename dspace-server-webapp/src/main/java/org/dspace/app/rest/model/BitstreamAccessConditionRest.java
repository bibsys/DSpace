package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.app.rest.RestResourceController;

import java.util.List;
import java.util.UUID;

/** The BitstreamAccessConditions REST Resource
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class BitstreamAccessConditionRest extends BaseObjectRest<UUID> {
    public static final String NAME = "bitstreamaccesscondition";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private List<AccessConditionDTO> policies;
    private AccessConditionDTO masterPolicy;

    public List<AccessConditionDTO> getPolicies() { return this.policies; }
    public void setPolicies(List<AccessConditionDTO> inPolicies) { this.policies = inPolicies; }

    @JsonInclude(Include.NON_NULL)
    public AccessConditionDTO getMasterPolicy() { return this.masterPolicy; }
    public void setMasterPolicy(AccessConditionDTO policy) { this.masterPolicy = policy; }

    @JsonIgnore
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    @JsonIgnore
    public Class getController() {
        return RestResourceController.class;
    }
}
