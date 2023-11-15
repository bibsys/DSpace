package org.dspace.uclouvain.administer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;

/**
 * A command-line tool for user group management. With this command we can
 * create or delete an existing user group.
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.GroupManagement -a [action] -n [name] [--force]
 *
 * ARGUMENTS:
 *   -n, --name:  the group name
 *   -a, --action: the action name ('create' or 'delete')
 *   -f, --force: force the action (only with 'delete' action)
 *   -h, --help:   display user-group-management options.
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 * @version $Revision$
 */
public class GroupManagement extends AbstractCLICommand {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** CLI available options */
    private static final Option OPT_HELP = Option.builder("h")
            .longOpt("help")
            .hasArg(false)
            .desc("explain group-management options")
            .build();
    private static final Option OPT_NAME = Option.builder("n")
            .longOpt("name")
            .hasArg(true)
            .desc("name of the group")
            .required(true)
            .build();
    private static final Option OPT_CREATE = Option.builder("a")
            .longOpt("action")
            .hasArg(true)
            .desc("action to perform (create|delete)")
            .required(true)
            .build();
    private static final Option OPT_FORCE = Option.builder("f")
            .longOpt("force")
            .hasArg(false)
            .desc("force operation")
            .required(false)
            .build();

    public static final String USAGE_DESCRIPTION = "A command-line tool for managing user's group";
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_DELETE = "delete";

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final Context context;
    protected EPersonService ePersonService;
    protected GroupService groupService;

    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * For invoking via the command line.
     *
     * @param argv: the command line arguments given
     * @throws MissingArgumentException : If a required argument is missing.
     */
    public static void main(String[] argv) throws Exception {
        GroupManagement gm = new GroupManagement();
        CommandLine cl = gm.validateCLIArgument(argv);
        gm.manageGroup(
                cl.getOptionValue("a"),
                cl.getOptionValue("n"),
                cl.hasOption("f")
        );
    }

    /**
     * constructor, which just creates an object with a ready context
     */
    protected GroupManagement() {
        context = new Context();
        groupService = EPersonServiceFactory.getInstance().getGroupService();
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_NAME);
        serviceOptions.addOption(OPT_CREATE);
        serviceOptions.addOption(OPT_FORCE);

        infoOptions.addOption(OPT_HELP);
    }
    protected String getUsageDescription() {
        return GroupManagement.USAGE_DESCRIPTION;
    }

    // PRIVATE FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Perform a management group action
     *
     * @param action: the action to execute (see constants of this class to find available actions)
     * @param name: the group name.
     * @param force: specify if the operation should be forced.
     * @throws Exception if any exceptions occurred during process.
     */
    private void manageGroup(String action, String name, boolean force) throws Exception {
        context.turnOffAuthorisationSystem();
        switch (action) {
            case ACTION_CREATE:
                createGroup(name);
                break;
            case ACTION_DELETE:
                //TODO :: create the delete action (using `force` argument)
                throw new UnsupportedOperationException();
            default:
                throw new IllegalArgumentException("Unknown action :: "+action);
        }
        context.complete();
    }

    /**
     * Create a new group
     * @param name: the name of the group to create.
     * @throws Exception if a group with same name already exists.
     */
    private void createGroup(String name) throws Exception {
        if (groupService.findByName(context, name) != null) {
            throw new Exception("Group already exists");
        }
        Group g = groupService.create(context);
        groupService.setName(g, name);
        groupService.update(context, g);
    }
}
