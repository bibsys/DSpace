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
