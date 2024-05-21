/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.List;

import org.dspace.authorize.ResourcePolicy;

/** Wrapper class to allow `List<ResourcePolicy>` to be converted using classic converter
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class BitstreamAccessConditions {

    private List<ResourcePolicy> policies;
    public List<ResourcePolicy> getPolicies() {
        return this.policies;
    }
    public void setPolicies(List<ResourcePolicy> inPolicies) {
        this.policies = inPolicies;
    }
}
