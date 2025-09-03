/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.exceptions;

public class EmailGenerationException extends Exception {
    public EmailGenerationException(String message) {
        super(message);
    }

    public EmailGenerationException(String message, Exception e) {
        super(message, e);
    }
}
