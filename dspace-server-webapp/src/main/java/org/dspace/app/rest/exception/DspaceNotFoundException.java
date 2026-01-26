/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * When a request is send to an existing endpoint but we don't want the user to know that the endpoint exists.
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Not Found")
public class DspaceNotFoundException extends RuntimeException {
    public DspaceNotFoundException(String message) {
        super(message);
    }

    public DspaceNotFoundException(String message, Exception e) {
        super(message, e);
    }
}
