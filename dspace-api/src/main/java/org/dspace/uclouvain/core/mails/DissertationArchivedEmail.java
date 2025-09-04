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
 * Notify the authors of a dissertation when it is validated and it enters the archive.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class DissertationArchivedEmail extends DissertationNotifyEmail {
    protected final List<String> fieldsToExpose = Arrays.asList(getConfigurationAttributes("metadata"));

    protected static final String CONFIG_NAME = "dissertation_notify_archive";
    protected static final String TEMPLATE_PATH = "/config/emails/dissertation_notify_archive";

    public DissertationArchivedEmail(Context context, Item item) throws EmailFailedInitException {
        super(context, item);
    }

    @Override
    protected String getConfigurationName() {
        return CONFIG_NAME;
    }

    @Override
    protected String getTemplatePath() {
        return this.source + TEMPLATE_PATH;
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
