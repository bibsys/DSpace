/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.exceptions;

/**
 * Email exception thrown when a {@link org.dspace.uclouvain.core.mails.UCLouvainEmail} failed to initialized.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class EmailFailedInitException extends Exception {
    public EmailFailedInitException(String message) {
        super(message);
    }

    public EmailFailedInitException(String message, Exception e) {
        super(message, e);
    }
}