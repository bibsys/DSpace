/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks;

import org.dspace.content.Item;

public class CitationGenerationException extends Exception {

    // DEV NOTE: Why defines an error prefix ?
    //   Using this prefix, we could determine if a rendered citation text produces an error or not
    public static final String ERROR_PREFIX = "Error generating citation";

    public String style;
    public Item item;

    public CitationGenerationException(String style, Item item) {
        this.style = style;
        this.item = item;
    }

    public String getMessage() {
        return String.format("%s for %s using %s", ERROR_PREFIX, item.getID(), style);
    }
}
