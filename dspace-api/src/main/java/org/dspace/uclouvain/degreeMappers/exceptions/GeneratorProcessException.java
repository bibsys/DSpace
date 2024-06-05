/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.degreeMappers.exceptions;

/**
 * An error thrown when the `process` method of a `MetadataGenerator` class fails.
 */
public class GeneratorProcessException extends Exception {

    public GeneratorProcessException(String message) {
        super(message);
    }
    public GeneratorProcessException(String message, Exception cause) {
        super(message, cause);
    }
    public GeneratorProcessException(Exception cause) {
        super(cause);
    }
}
