/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.citations;

/**
 * Error thrown when an invalid citation format is used to generate a citation for an item.
 * 
 * @author Michaël Pourbaix (michael.pourbaiax@uclouvain.be)
 */
public class UnknownCitationFormatException extends Exception {
    UnknownCitationFormatException(String message) {
        super(message);
    }

    UnknownCitationFormatException(String message, Exception e) {
        super(message, e);
    }
}
