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
import org.dspace.core.Email;
import org.dspace.eperson.EPerson;
import org.dspace.uclouvain.exceptions.EmailFailedInitException;
import org.dspace.uclouvain.exceptions.EmailGenerationException;

/**
 * Email to send to submitter and authors of a publication when it is withdrawn.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class PublicationNotifyAuthorWithdrawEmail extends PublicationNotifyAuthorsEmail {
    protected EPerson provenanceUser;

    protected final List<String> fieldsToExpose = Arrays.asList(getConfigurationAttributes("metadata"));

    public PublicationNotifyAuthorWithdrawEmail(
        Context context, Item item, EPerson user
    ) throws EmailFailedInitException {
        super(context, item);
        this.provenanceUser = user;
    }

    @Override
    protected String getConfigurationName() {
        return "notify_withdraw_authors";
    }

    @Override
    protected String getTemplatePath() {
        return this.source + "/config/emails/publication_notify_withdraw_authors";
    }

    @Override
    protected void generateEmail(Email email, Item item) throws EmailGenerationException {
        try {
            email.addArgument(getHandle(context, item));
            email.addArgument(provenanceUser.getEmail());
            email.addArgument(mailMetadataParserService.parseMetadata(context, item, fieldsToExpose, "fr"));
            email.addArgument(mailMetadataParserService.parseMetadata(context, item, fieldsToExpose, "en"));
        } catch (Exception e) {
            throw new EmailGenerationException("An error occurred while filling email informations", e);
        }
    }
}
