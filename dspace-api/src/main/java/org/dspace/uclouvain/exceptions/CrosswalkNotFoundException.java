/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.exceptions;

import org.dspace.content.crosswalk.CrosswalkException;

/**
 * Exception to specify that a Crosswalk with the given identifier could not be found.
 */
public class CrosswalkNotFoundException extends CrosswalkException {
    public CrosswalkNotFoundException(String message) {
        super(message);
    }
}
