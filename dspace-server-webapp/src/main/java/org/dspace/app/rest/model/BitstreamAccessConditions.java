package org.dspace.app.rest.model;

import org.dspace.authorize.ResourcePolicy;
import java.util.List;

/** Wrapper class to allow `List<ResourcePolicy>` to be converted using classic converter
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class BitstreamAccessConditions {

    private List<ResourcePolicy> policies;
    public List<ResourcePolicy> getPolicies() { return this.policies; }
    public void setPolicies(List<ResourcePolicy> inPolicies) { this.policies = inPolicies; }
}
