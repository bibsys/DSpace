/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.citations;

/**
 * Internal class for working with an item citation. Very similar to the Rest model.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ItemCitation {
    private String format;
    private String citation;

    ItemCitation(String format, String citation) {
        this.format = format;
        this.citation = citation;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCitation() {
        return citation;
    }

    public void setCitation(String citation) {
        this.citation = citation;
    }
}
