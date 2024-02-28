package org.dspace.uclouvain.submissionMetadataGenerators.exceptions;

/**
 * An error thrown when the initial degree code retrieved from the frontend is not valid.
 */
public class MalformedDegreeCodeException extends Exception {
    public MalformedDegreeCodeException(String message) {
        super(message);
    }

    public MalformedDegreeCodeException(String message, Exception cause) {
        super(message, cause);
    }

    public MalformedDegreeCodeException(Exception cause) {
        super(cause);
    }
}
