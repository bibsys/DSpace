package org.dspace.uclouvain.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.authorize.ResourcePolicy;

/**
 * This is a class that returns resource policy information for a REST API response.
 */
public class ResourcePolicyRestResponse {
    public List<ResourcePolicyRestModel> restPolicies = new ArrayList<ResourcePolicyRestModel>();
    public ResourcePolicyRestModel masterPolicy;

    /**
     * Main constructor for the ResourcePolicyRestResponse class.
     * It filters the policies by type and creates a list of ResourcePolicyRestModel objects.
     * @param policies: The list of resource policies to use to instantiate the classes.
     * @param type: A type to filter the policies by.
     */
    public ResourcePolicyRestResponse(List<ResourcePolicy> policies, String type) {
        this(policies.stream().filter(p -> p.getRpType().equals(type)).collect(Collectors.toList()));
    }

    /**
     * Main constructor for the ResourcePolicyRestResponse class.
     * It creates a list of ResourcePolicyRestModel objects without any filtering.
     * @param policies: The list of resource policies to use to instantiate the classes.
     */
    public ResourcePolicyRestResponse(List<ResourcePolicy> policies) {
        policies.forEach(p -> this.restPolicies.add(new ResourcePolicyRestModel(p)));
    }
}
