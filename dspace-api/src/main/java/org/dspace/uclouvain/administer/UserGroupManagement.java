package org.dspace.uclouvain.administer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;

/**
 * A command-line tool for managing membership users into groups. With this
 * command we can add or remove an existing user into/from an existing group.
 * If we try to add a user into a group, but this user is already member of the
 * group, nothing happens. The user isn't twice member of the group.
 * Similar behavior happens if we try to delete a user from a group but this
 * user isn't yet member of this group.
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.UserGroupManagement  -u [user] -g [group] -a [action]
 *
 * ARGUMENTS:
 *   -u, --user:   the user email or user UUID (aka NetID)
 *   -g, --group:  the group name
 *   -a, --action: the action name ('add' or 'delete')
 *   -h, --help:   display user-group-management options.
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 * @version $Revision$
 */
public class UserGroupManagement extends AbstractCLICommand {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** CLI available options */
    private static final Option OPT_USER = Option.builder("u")
            .longOpt("user")
            .hasArg(true)
            .desc("user email address OR user UUID")
            .required(true)
            .build();
    private static final Option OPT_GROUP = Option.builder("g")
            .longOpt("group")
            .hasArg(true)
            .desc("name of the group to manage")
            .required(true)
            .build();
    private static final Option OPT_ACTION = Option.builder("a")
            .longOpt("action")
            .hasArg(true)
            .desc("action to execute (add|delete)")
            .required(true)
            .build();
    private static final Option OPT_FORCE = Option.builder("f")
            .longOpt("force")
            .hasArg(false)
            .desc("force group creation if not exists")
            .required(false)
            .build();

    public static final String USAGE_DESCRIPTION = "A command-line tool for managing the groups a user belongs to";
    public static final String ACTION_ADD = "add";
    public static final String ACTION_DELETE = "delete";

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final Context context = new Context();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();


    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * For invoking via the command line. If called with no command line arguments,
     * it will negotiate with the user for the administrator details
     *
     * @param argv: the command line arguments given
     * @throws MissingArgumentException: If a required argument is missing.
     */
    public static void main(String[] argv) throws Exception {
        UserGroupManagement ugm = new UserGroupManagement();
        CommandLine cl = ugm.validateCLIArgument(argv);
        ugm.manageGroupMembership(
                cl.getOptionValue("a"),
                cl.getOptionValue("u"),
                cl.getOptionValue("g"),
                cl.hasOption("f")
        );
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_ACTION);
        serviceOptions.addOption(OPT_GROUP);
        serviceOptions.addOption(OPT_USER);
        serviceOptions.addOption(OPT_FORCE);
        infoOptions.addOption(OPT_HELP);
    }
    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    // PRIVATE FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Perform a management group action
     *
     * @param action: the action to execute (see constants of this class to find available actions)
     * @param userID: the user's identifier use to retrieve the ePerson.
     * @param groupName: the group name to manage.
     * @throws Exception if any exceptions occurred during process.
     */
    private void manageGroupMembership(String action, String userID, String groupName, boolean force) throws Exception{
        context.turnOffAuthorisationSystem();
        EPerson eperson = UtilityCLITool.findUser(context, userID);
        Group group = getGroup(groupName, force);
        switch (action) {
            case ACTION_ADD:
                groupService.addMember(context, group, eperson);
                break;
            case ACTION_DELETE:
                groupService.removeMember(context, group, eperson);
                break;
            default:
                throw new IllegalArgumentException("Unknown action :: "+action);
        }
        groupService.update(context, group);
        context.complete();
    }



    /**
     * Get a group based on its name. If this group doesn't yet exist, it could be
     * created.
     *
     * @param groupName: the group name
     * @param createGroup: If group doesn't yet exist, should it be created ?
     * @return the corresponding group
     * @throws IllegalStateException if a database exception occurred or no group is found
     */
    private Group getGroup(String groupName, boolean createGroup) throws Exception {
        Group group = this.groupService.findByName(this.context, groupName);
        if (group == null) {
            if (createGroup) {
                group = this.groupService.create(context);
                groupService.setName(group, groupName);
                // DEV NOTES :: not need to update `groupService` because it will be
                // triggered at the end of the CLI (if no errors occurred).
            } else {
                throw new IllegalStateException("Group not found");
            }
        }
        return group;
    }
}