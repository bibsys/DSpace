/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.administer;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.content.snapshot.tasks.SnapshotDetectingChangesTask;
import org.dspace.uclouvain.core.NotificationType;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.ItemSnapshotService;
import org.dspace.utils.DSpace;

/**
 * A command-line tool to run item snapshot changes detection
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.SnapshotCLI [--silent] [--notify TYPE] [--limit n] [UUID, UUID]
 *
 * ARGUMENTS:
 *   -s, --silent: detect changes without warning anyone
 *   -n, --notify: the channel to use to warn recipients; defaults to EMAIL when neither this option nor
 *                 `--silent` is given
 *   -l, --limit: determine the maximum number of publication to perform
 *   UUID: the list of item UUID to analyze. If not present, the system will determine itself which item should be
 *         analyzed
 *
 * @author Renaud Michotte &lt;renaud.michotte@uclouvain.be&gt;
 * @version $Revision$
 */
public class SnapshotCLI extends AbstractCLICommand  {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** Notifying recipients is the norm: it takes an explicit `--silent` to stay quiet */
    private static final NotificationType DEFAULT_NOTIFICATION = NotificationType.EMAIL;

    /** CLI available options */
    private static final Option OPT_SILENT = Option.builder("s")
        .longOpt("silent")
        .hasArg(false)
        .desc("detect changes without warning any recipient")
        .build();
    private static final Option OPT_NOTIFY = Option.builder("n")
        .longOpt("notify")
        .hasArg(true)
        .argName("type")
        .desc("channel used to warn recipients, one of " + notificationTypeNames()
            + "; defaults to " + DEFAULT_NOTIFICATION)
        .build();
    private static final Option OPT_LIMIT = Option.builder("l")
        .longOpt("limit")
        .hasArg(true)
        .argName("number")
        .desc("Limit the maximum number of elements processed (use -1 for no limit)")
        .type(Integer.class)
        .build();
    public static final String USAGE_DESCRIPTION = "A command-line tool to run item snapshot changes detection";

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final ItemSnapshotService snapshotService;
    private final int configuredItemsLimit;

    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** constructor, which just creates an object with a ready context. */
    protected SnapshotCLI() {
        snapshotService = UCLouvainServiceFactory.getInstance().getSnapshotService();
        configuredItemsLimit = DSpaceServicesFactory.getInstance()
            .getConfigurationService()
            .getIntProperty("item-snapshot.scheduled-task.items-limit", -1);
    }

    /**
     * For invoking via the command line.
     *
     * @param argv the command line arguments given
     * @throws Exception If the detection process failed.
     */
    public static void main(String[] argv) throws Exception {
        SnapshotCLI cli = new SnapshotCLI();
        CommandLine cl = cli.validateCLIArgument(argv);
        cli.run(
            parseItemIds(cl),
            parseNotificationType(cl),
            parseItemsLimit(cl, cli.configuredItemsLimit)
        );
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_SILENT);
        serviceOptions.addOption(OPT_NOTIFY);
        serviceOptions.addOption(OPT_LIMIT);
        infoOptions.addOption(OPT_HELP);
    }

    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    /**
     * Reject invalid arguments here rather than let them surface as a raw stack trace: this method reports them
     * through the regular usage message.
     *
     * @param cl The parsed command line (containing arguments)
     * @throws ParseException If a validation error is detected.
     */
    @Override
    protected void extraValidationCLIArgument(CommandLine cl) throws ParseException {
        if (cl.hasOption(OPT_SILENT) && cl.hasOption(OPT_NOTIFY)) {
            throw new ParseException("`--silent` and `--notify` are mutually exclusive.");
        }
        if (cl.hasOption(OPT_NOTIFY)) {
            String requested = cl.getOptionValue(OPT_NOTIFY);
            try {
                NotificationType.valueOf(requested.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ParseException(
                        "Unknown notification type '" + requested + "'; expected one of " + notificationTypeNames());
            }
        }
        if (cl.hasOption(OPT_LIMIT)) {
            // Triggers the Integer conversion, so that a non-numeric value is reported here
            cl.getParsedOptionValue(OPT_LIMIT);
        }
        for (String argument : cl.getArgList()) {
            try {
                UUID.fromString(argument);
            } catch (IllegalArgumentException e) {
                throw new ParseException("'" + argument + "' is not a valid item UUID.");
            }
        }
    }

    // PRIVATE FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** The readable list of the notification channels a user may ask for */
    private static String notificationTypeNames() {
        return Arrays.stream(NotificationType.values()).map(Enum::name).collect(Collectors.joining(", "));
    }

    /** The items explicitly requested on the command line, empty when the tool has to determine them itself */
    private static List<UUID> parseItemIds(CommandLine cl) {
        return cl.getArgList().stream().map(UUID::fromString).toList();
    }

    /** The channel to warn recipients through, or null when `--silent` was requested */
    private static NotificationType parseNotificationType(CommandLine cl) {
        if (cl.hasOption(OPT_SILENT)) {
            return null;
        }
        return cl.hasOption(OPT_NOTIFY)
            ? NotificationType.valueOf(cl.getOptionValue(OPT_NOTIFY).toUpperCase())
            : DEFAULT_NOTIFICATION;
    }

    /**
     * An explicit `--limit` always wins, including `-1` (unlimited); the configured value applies otherwise.
     *
     * @param cl the parsed command line
     * @param fallback the limit to apply when the option is absent
     * @return the maximum number of items to look up
     */
    protected static int parseItemsLimit(CommandLine cl, int fallback) throws ParseException {
        return cl.hasOption(OPT_LIMIT)
            ? (Integer) cl.getParsedOptionValue(OPT_LIMIT)
            : fallback;
    }

    /**
     * Detect the changes, and warn the recipients unless asked to stay silent.
     *
     * @param itemIds the items to analyze; when empty, the eligible ones are looked up
     * @param notifyBy the channel to warn recipients through, null to warn nobody
     * @param itemsLimit the maximum number of items to look up
     */
    private void run(List<UUID> itemIds, NotificationType notifyBy, int itemsLimit) throws SQLException {
        // `performItems` commits every item as it goes, but the reads performed around it leave a transaction -- and
        // its database connection -- open. `Context` is AutoCloseable exactly for that: on the way out, `complete()`
        // has already invalidated it and closing is a no-op, whereas on an error the connection is rolled back and
        // released.
        try (Context session = context) {
            List<UUID> items = itemIds.isEmpty()
                ? snapshotService.findItemsToSnapshot(session, null, itemsLimit)
                : itemIds;

            SnapshotDetectingChangesTask task = new DSpace()
                .getServiceManager()
                .getApplicationContext()
                .getBean(SnapshotDetectingChangesTask.class);

            task.performItems(session, items, notifyBy);
            session.complete();
        }
    }
}
