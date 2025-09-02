/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;
import org.dspace.uclouvain.exceptions.EmailGenerationException;

/**
 * Generic class to create a master thesis email.
 * In order to use this email, you will need to provide an item of type 'MasterThesis'.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract class GenericThesisEmail extends AbstractUCLouvainEmail {

    // ATTRIBUTES ======================================================================================================
    protected String supervisorEmailField = configService.getProperty(
            "uclouvain.global.metadata.advisoremail.field", "advisors.email");
    protected String authorEmailField = configService.getProperty(
            "uclouvain.global.metadata.authoremail.field", "authors.email");

    // ABSTRACT METHODS ================================================================================================
    protected abstract String getTemplatePath();
    protected abstract String buildMailSubject();
    protected abstract void generateEmail(Email email, Item item) throws EmailGenerationException;
    protected abstract String getConfigurationName();
    protected abstract List<String> getRecipientAddresses();

    // METHODS =========================================================================================================
    public GenericThesisEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
    }

    /**
     * Verify that the email can handle a given item.
     * @param context The current DSpace context.
     * @param item The item to check the validity of.
     * @return A boolean indicating if the mail can handle the given item.
     */
    public boolean isValidForItem(Context context, Item item) {
        return itemService.getEntityType(item).equals("MasterThesis");
    }
}
