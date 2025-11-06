/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.exceptions;

import org.dspace.content.Item;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;

public class PublicationSetAuthorException extends Exception {
    private final Item item;
    private final PublicationAuthor author;

    public PublicationSetAuthorException(Item item, PublicationAuthor author) {
        this.item = item;
        this.author = author;
    }

    public String getMessage() {
        return "Could not add author %s to publication with id %s".formatted(author.getName(), item.getID());
    }
}
