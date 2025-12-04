/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemValidators.exceptions;

/**
 * RuntimeException to throw when an item was considered not valid and so cannot be created/updated.
 * 
 * DEV_NOTE: We use a RuntimeException so that we are not forced to handle it in the calling classes.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ItemValidationException extends RuntimeException {
    // Store a string to use in the frontend for translation purposes.
    protected String translatableMessage;

    public ItemValidationException(String message, String translatableMessage) {
        super(message);
        this.translatableMessage = translatableMessage;
    }

    // GETTERS && SETTERS
    public String getTranslatableMessage() {
        return translatableMessage;
    }
}
