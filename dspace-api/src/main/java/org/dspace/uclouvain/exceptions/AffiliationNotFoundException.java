/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.exceptions;

import java.util.NoSuchElementException;

/**
 * Throw this error when an affiliation cannot be found using some identifiers.
 */
public class AffiliationNotFoundException extends NoSuchElementException {
    public AffiliationNotFoundException(String message) {
        super(message);
    }

    public AffiliationNotFoundException(String message, Exception e) {
        super(message, e);
    }
}
