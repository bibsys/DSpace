/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external;

/**
 * An external source (CrossRef, PubMed, arXiv...) could not be reached or answered with an error. A "not found"
 * answer is not an error: {@link ExternalSourceClient} reports it as a null response.
 */
public class ExternalSourceException extends RuntimeException {

    public ExternalSourceException(String message) {
        super(message);
    }

    public ExternalSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
