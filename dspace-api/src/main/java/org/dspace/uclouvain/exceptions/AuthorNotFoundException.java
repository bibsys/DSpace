/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.exceptions;

/**
 * Throw this error when an author cannot be found using some identifiers.
 */
public class AuthorNotFoundException extends Exception {
    public AuthorNotFoundException(String message) {
        super(message);
    }

    public AuthorNotFoundException(String message, Exception e) {
        super(message, e);
    }
}
