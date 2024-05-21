/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model;

/** Class to represent a ResourcePolicy priority.
 *  It allows determining a primary policy if multiple policies are defined on the same DSpaceObject
 *  (Item, Bitstream, ...)
 *  More the weight is high, more the priority is important
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class ResourcePolicyPriority {

    private String rpName;
    private int weight;

    public String getRpName() {
        return rpName;
    }
    public void setRpName(String rpName) {
        this.rpName = rpName;
    }

    public int getWeight() {
        return weight;
    }
    public void setWeight(int weight) {
        this.weight = weight;
    }
}