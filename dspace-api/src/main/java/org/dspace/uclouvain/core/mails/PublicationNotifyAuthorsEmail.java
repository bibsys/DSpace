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
import org.dspace.uclouvain.exceptions.EmailFailedInitException;
import org.dspace.uclouvain.exceptions.EmailGenerationException;
/**
 * Mail to notify the authors of a publication that it has been submitted.
 */
public class PublicationNotifyAuthorsEmail extends AbstractNotifyEmail {

    protected final List<String> fieldsToExpose = Arrays.asList(getConfigurationAttributes("metadata"));

    public PublicationNotifyAuthorsEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
    }

    @Override
    protected String getConfigurationName() {
        return "notify_authors";
    }

    @Override
    protected String getTemplatePath() {
        return this.source + "/config/emails/publication_notify_authors";
    }

    @Override
    protected void generateEmail(Email email, Item item) throws EmailGenerationException {
        try {
            email.addArgument(getHandle(context, item));
            email.addArgument(mailMetadataParserService.parseMetadata(context, item, fieldsToExpose, "fr"));
            email.addArgument(mailMetadataParserService.parseMetadata(context, item, fieldsToExpose, "en"));
            email.addArgument(getAttachedFiles(context, item));
        } catch (Exception e) {
            throw new EmailGenerationException("An error occurred while filling email informations", e);
        }
    }
}
