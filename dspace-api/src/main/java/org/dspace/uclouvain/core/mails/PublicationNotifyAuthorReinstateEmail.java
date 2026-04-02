/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails;

import java.util.Arrays;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;

/**
 * Email to send to submitter and authors of a publication when it is reinstated.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class PublicationNotifyAuthorReinstateEmail extends PublicationNotifyAuthorWithdrawEmail {
    protected final List<String> fieldsToExpose = Arrays.asList(getConfigurationAttributes("metadata"));

    public PublicationNotifyAuthorReinstateEmail(
        Context context, Item item, EPerson user
    ) throws EmailFailedInitException {
        super(context, item, user);
    }

    @Override
    protected String getConfigurationName() {
        return "notify_reinstate_authors";
    }

    @Override
    protected String getTemplatePath() {
        return this.source + "/config/emails/publication_notify_reinstate_authors";
    }
}
