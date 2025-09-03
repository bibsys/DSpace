/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import org.dspace.uclouvain.exceptions.EmailGenerationException;
import org.dspace.uclouvain.exceptions.SendEmailException;

/**
 * Main interface that all UCLouvain mails should implement.
 * You can send an email using the `sendEmail()` method.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public interface UCLouvainEmail {
    /**
     * Sends an initialized email to the configured recipients.
     * @throws EmailGenerationException If the content of the email could not be generated.
     * @throws SendEmailException If the email could not be sent.
     */
    public void sendEmail() throws EmailGenerationException, SendEmailException;
}
