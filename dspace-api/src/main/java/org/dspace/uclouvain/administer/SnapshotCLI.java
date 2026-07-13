/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.administer;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.james.mime4j.field.structured.parser.ParseException;
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
 *   dspace dsrun org.dspace.uclouvain.administer.SnapshotCLI [--notify] [UUID, UUID]
 *
 * ARGUMENTS:
 *   -n, --notify: if this argument is present, snapshot recipients will be notified; otherwise not
 *   -l, --limit: determine the maximum number of publication to perform
 *   UUID: the list of item UUID to analyze. If not present, the system will determine itself which item should be
 *         analyzed
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 * @version $Revision$
 */
public class SnapshotCLI extends AbstractCLICommand  {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** CLI available options */
    private static final Option OPT_NOTIFY = Option.builder("n")
        .longOpt("notify")
        .hasArg(false)
        .desc("snapshot recipients will be notified for changes")
        .build();
    private static final Option OPT_LIMIT = Option.builder("l")
        .longOpt("limit")
        .hasArg(true)
        .argName("number")
        .desc("Limit the maximum number of elements processed")
        .type(Integer.class)
        .build();
    public static final String USAGE_DESCRIPTION = "A command-line tool to run item snapshot changes detection";

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final Context context;
    private final ItemSnapshotService snapshotService;
    private int itemsLimit;

    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** constructor, which just creates an object with a ready context. */
    protected SnapshotCLI() {
        context = new Context();
        snapshotService = UCLouvainServiceFactory.getInstance().getSnapshotService();
        itemsLimit = DSpaceServicesFactory.getInstance()
            .getConfigurationService()
            .getIntProperty("item-snapshot.items-limit", -1);
    }

    /**
     * For invoking via the command line.
     *
     * @param argv the command line arguments given
     * @throws MissingArgumentException If a required argument is missing.
     */
    public static void main(String[] argv) throws Exception {
        SnapshotCLI cli = new SnapshotCLI();
        CommandLine cl = cli.validateCLIArgument(argv);
        List<UUID> uuids = cl.getArgList().stream().map(UUID::fromString).toList(); // leftover arguments
        int optLimit = -1;
        if (cl.hasOption(OPT_LIMIT)) {
            Integer parsedValue = cl.getParsedOptionValue(OPT_LIMIT);
            if (parsedValue != null) {
                optLimit = parsedValue;
            }
        }
        cli.run(uuids, cl.hasOption('n'), optLimit);
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_NOTIFY);
        serviceOptions.addOption(OPT_LIMIT);
        infoOptions.addOption(OPT_HELP);
    }
    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    // PRIVATE FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void run (List<UUID> uuids, boolean notifyRecipients, int itemsLimit) throws SQLException {
        itemsLimit = (itemsLimit > 0) ? itemsLimit : this.itemsLimit;
        if (uuids == null || uuids.isEmpty()) {
            uuids = snapshotService.findItemsToSnapshot(context, null, itemsLimit);
        }

        SnapshotDetectingChangesTask task = (SnapshotDetectingChangesTask) (new DSpace()
            .getServiceManager()
            .getApplicationContext()
            .getBean(SnapshotDetectingChangesTask.class)
        );

        task.performItems(context, uuids, notifyRecipients ? NotificationType.EMAIL : null);
    }
}
