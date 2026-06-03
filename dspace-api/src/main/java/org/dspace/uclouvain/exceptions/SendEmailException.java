/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.exceptions;

/**
 * An email exception thrown when a {@link org.dspace.uclouvain.core.mails.UCLouvainEmail} could not be sent.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class SendEmailException extends Exception {
    public SendEmailException(String message) {
        super(message);
    }

    public SendEmailException(String message, Exception e) {
        super(message, e);
    }
}