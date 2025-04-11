/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.citations;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal class for working with a list of item citations. Very similar to the Rest model.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ItemCitations {
    List<ItemCitation> citations = new ArrayList<>();

    public List<ItemCitation> getCitations() {
        return this.citations;
    }

    public void setCitations(List<ItemCitation> citations) {
        this.citations = citations;
    }

    public void addCitation(String format, String value) {
        this.citations.add(new ItemCitation(format, value));
    }
}
