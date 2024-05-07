package org.dspace.uclouvain.core.model;

import java.util.Date;

import org.dspace.authorize.ResourcePolicy;

// This model represents a resource policy to be returned as an API call response.
public class ResourcePolicyRestModel {

    public ResourcePolicyRestModel(ResourcePolicy resourcePolicy) {
        this.id = resourcePolicy.getID().toString();
        this.name = resourcePolicy.getRpName();
        this.description = resourcePolicy.getRpDescription();
        this.policyType = resourcePolicy.getRpType();
        this.action = resourcePolicy.getAction();
        this.startDate = resourcePolicy.getStartDate();
        this.endDate = resourcePolicy.getEndDate();
        this.type = resourcePolicy.getGroup().getName();
    }

    public String id;
    public String name;
    public String description;
    public String policyType;
    public int action;
    public Date startDate;
    public Date endDate;
    public String type;
}
