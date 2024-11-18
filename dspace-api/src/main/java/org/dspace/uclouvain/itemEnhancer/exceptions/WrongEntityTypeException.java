/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.exceptions;

/**
 * Generic exception thrown when an item does not have the desired entity type.
 */
public class WrongEntityTypeException extends Exception {
    public WrongEntityTypeException (String message) {
        super(message);
    }
}
