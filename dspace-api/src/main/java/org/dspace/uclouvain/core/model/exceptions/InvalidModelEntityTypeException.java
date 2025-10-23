/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.exceptions;

import org.dspace.content.Item;

public class InvalidModelEntityTypeException extends Exception {

    private final Item item;
    private final String entityType;

    public InvalidModelEntityTypeException(Item item, String entityType) {
        this.item = item;
        this.entityType = entityType;
    }

    public String getMessage() {
        return "%s doesn't match [%s] entity type".formatted(item, entityType);
    }
}
