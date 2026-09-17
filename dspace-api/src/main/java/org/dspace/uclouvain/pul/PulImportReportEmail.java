/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import jakarta.mail.MessagingException;
import org.dspace.core.Email;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * The end-of-run report of the {@code pul-import} script, emailed to the addresses of
 * {@code pul.import.report.recipients}. Nothing is sent when no recipient is configured.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class PulImportReportEmail {

    public static final String RECIPIENTS_PROPERTY = "pul.import.report.recipients";
    public static final String SUBJECT_PROPERTY = "pul.import.report.subject";
    private static final String TEMPLATE_PATH = "/config/emails/pul_import_report";

    private final ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
        .getConfigurationService();

    /** @return the configured recipients; empty means the report is not sent. */
    public List<String> recipients() {
        return List.of(configurationService.getArrayProperty(RECIPIENTS_PROPERTY, new String[0]));
    }

    /**
     * @param directory the directory that was read.
     * @param dryRun    whether the run wrote anything.
     * @param summary   the counters per decision, e.g. {@code {CREATE=98, UPDATE=3}}.
     * @param lines     one report line per file.
     * @throws IOException        if the template cannot be read.
     * @throws MessagingException if the mail cannot be sent.
     */
    public void send(String directory, boolean dryRun, String summary, List<String> lines)
        throws IOException, MessagingException {
        Email email = Email.getEmail(configurationService.getProperty("dspace.dir") + TEMPLATE_PATH);
        email.setSubject(configurationService.getProperty(SUBJECT_PROPERTY, "[DIAL.pr] PUL import report")
            + (dryRun ? " (dry run)" : ""));
        recipients().forEach(email::addRecipient);
        email.addArgument(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm:ss")));
        email.addArgument(directory);
        email.addArgument(dryRun ? "dry run" : "import");
        email.addArgument(summary);
        email.addArgument(String.join("\n", lines));
        email.send();
    }
}
