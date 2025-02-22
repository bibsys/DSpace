/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.xmlworkflow.actions;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.access.status.DefaultAccessStatusHelper;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.mail.MailMetadataParserService;
import org.dspace.xmlworkflow.state.Step;
import org.dspace.xmlworkflow.state.actions.ActionResult;
import org.dspace.xmlworkflow.state.actions.processingaction.ProcessingAction;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Action to notify all the authors of a publication when it is deposited.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class NotifyAuthorsAction extends ProcessingAction {
    protected static final Logger logger = LogManager.getLogger(NotifyAuthorsAction.class);
    private ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private String source = configService.getProperty("dspace.dir");
    private String authorEmailField =
        configService.getProperty("uclouvain.global.metadata.authoremail.field");

    @Autowired
    private ItemService itemService;
    @Autowired
    private AccessStatusService accessStatusService;
    @Autowired
    private MailMetadataParserService mailMetadataParserService;

    // Email data
    private String subject = configService.getProperty(
        "uclouvain.notify_authors.mail.subject",
        "[DIAL.PR] Author notification"
    );
    private String[] forcedRecipients =
        configService.getArrayProperty("uclouvain.notify_authors.mail.recipients", new String[0]);
    private final List<String> fieldsToExpose =
        Arrays.asList(configService.getArrayProperty("uclouvain.notify_authors.mail.metadata", new String[0]));

    @Override
    public void activate(Context c, XmlWorkflowItem wf) {}

    @Override
    public ActionResult execute(Context context, XmlWorkflowItem wfi, Step step, HttpServletRequest request) {
        final ActionResult result = new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, ActionResult.OUTCOME_COMPLETE);
        // 1. Retrieve the item.
        // 2. Extract all the authors from the item's metadata.
        // 3. Send a notification email to all the authors or configured addresses.
        Item item = wfi.getItem();
        if (item != null) {
            try {
                Email notificationEmail = Email.getEmail(source + "/config/emails/publication_notify_authors");
                notificationEmail.setSubject(subject);
                // Add the recipients for the email.
                addEmailRecipient(item, notificationEmail);
                // Add the current timestamp.
                notificationEmail.addArgument(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss"))
                );
                // Add item metadata in both french and english.
                notificationEmail.addArgument(
                    mailMetadataParserService.parseMetadata(context, item, fieldsToExpose, "fr")
                );
                notificationEmail.addArgument(
                    mailMetadataParserService.parseMetadata(context, item, fieldsToExpose, "en")
                );
                // Add bitstream information
                notificationEmail.addArgument(getAttachedFiles(context, item));
                // Send the email.
                notificationEmail.send();
            } catch (Exception e) {
                logger.error(
                    "Could not build or send the publication notification email",
                    e
                );
            }
        }
        return result;
    }

    private void addEmailRecipient(Item item, Email email) {
        // See if a config is present to override the recipients:
        // - If it is present use it.
        // - Else use the authors emails.
        if (forcedRecipients.length > 0) {
            for (String mail: forcedRecipients) {
                email.addRecipient(mail);
            }
        } else {
            itemService.getMetadataByMetadataString(item, authorEmailField).stream()
                .map(MetadataValue::getValue)
                .forEach(authorEmail -> email.addRecipient(authorEmail));
        }
    }

    private List<String[]> getAttachedFiles(Context context, Item item) {
        return item.getBundles(Constants.CONTENT_BUNDLE_NAME)
                .stream()
                .findFirst()
                .map(Bundle::getBitstreams).orElse(Collections.emptyList())
                .stream()
                .map(bitstream -> new String[]{
                        bitstream.getName(),
                        getBitstreamAccessStatus(context, bitstream)
                })
                .collect(Collectors.toList());
    }

    private String getBitstreamAccessStatus(Context context, Bitstream bitstream) {
        try {
            return accessStatusService.getBitstreamAccessStatus(context, bitstream);
        } catch (SQLException e) {
            return DefaultAccessStatusHelper.UNKNOWN;
        }
    }

    @Override
    public List<String> getOptions() {
        // No options for this action so return an empty list.
        return Collections.emptyList();
    }
}
