/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester.exceptions;

public class IDMCheckException extends Exception {
    public IDMCheckException(String message) {
        super(message);
    }

    public IDMCheckException(String message, Exception e) {
        super(message, e);
    }
}
