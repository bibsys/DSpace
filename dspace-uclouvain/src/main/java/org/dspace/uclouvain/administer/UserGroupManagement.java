package org.dspace.uclouvain.administer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;

import java.sql.SQLException;
import java.util.Arrays;

import static org.dspace.uclouvain.administer.UtilityCLITool.usage;

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
public class UserGroupManagement {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** CLI available options */
    private static final Option OPT_HELP = Option.builder("h")
            .longOpt("help")
            .hasArg(false)
            .desc("explain user-group-management options")
            .build();
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

    public static final String USAGE_DESCRIPTION = "A command-line tool for managing the groups a user belongs to";
    public static final String ACTION_ADD = "add";
    public static final String ACTION_DELETE = "delete";

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final Context context;
    protected EPersonService ePersonService;
    protected GroupService groupService;


    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * For invoking via the command line. If called with no command line arguments,
     * it will negotiate with the user for the administrator details
     *
     * @param argv: the command line arguments given
     * @throws MissingArgumentException: If a required argument is missing.
     */
    public static void main(String[] argv) throws Exception {

        Options infoOptions = new Options();
        infoOptions.addOption(OPT_HELP);

        Options serviceOptions = new Options();
        serviceOptions.addOption(OPT_USER);
        serviceOptions.addOption(OPT_GROUP);
        serviceOptions.addOption(OPT_ACTION);

        // Check command line arguments
        //    * First check if some information's options are present into CLI argument.
        //    * Next we could re-parse the argument to check service's options and try to execute the CLI.
        CommandLineParser parser = new DefaultParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(infoOptions, argv, true);
            if (cl.hasOption("help")) {
                throw new ParseException("");
            }
            cl = parser.parse(serviceOptions, argv, true);
        } catch (ParseException pe) {
            if (!pe.getMessage().isEmpty()) {
                System.err.println(pe.getMessage());
            }
            usage(UserGroupManagement.class,
                  UserGroupManagement.USAGE_DESCRIPTION,
                  Arrays.asList(infoOptions, serviceOptions)
            );
            System.exit(1);
        }
        // Execute the command
        UserGroupManagement ugm = new UserGroupManagement();
        ugm.manageGroupMembership(cl.getOptionValue("a"), cl.getOptionValue("u"), cl.getOptionValue("g"));
    }

    /**
     * constructor, which just creates and object with a ready context
     */
    protected UserGroupManagement() {
        context = new Context();
        groupService = EPersonServiceFactory.getInstance().getGroupService();
        ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
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
    private void manageGroupMembership(String action, String userID, String groupName) throws Exception{
        // This is a CLI command, no need to be authenticated
        context.turnOffAuthorisationSystem();

        EPerson eperson = findUser(userID);
        Group group = findGroup(groupName);
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
        System.out.println("Group '"+groupName+"' updated");
    }

    /**
     * Find an ePerson based either on email, either on UUID (aka. NetID)
     *
     * @param userID: the user's identifier
     * @return the corresponding ePerson
     * @throws IllegalStateException if a database exception occurred or no ePerson is found.
     */
    private EPerson findUser(String userID) throws IllegalStateException {
        if (userID == null || userID.isEmpty()) {
            throw new IllegalStateException("user ID must be specified");
        }
        try {
            EPerson eperson = this.ePersonService.findByEmail(this.context, userID);
            if (eperson != null) {
                return eperson;
            }
            eperson = this.ePersonService.findByNetid(this.context, userID);
            if (eperson != null) {
                return eperson;
            }
        } catch(SQLException e) {
            throw new IllegalStateException("User not found :: "+e.getMessage());
        }
        throw new IllegalStateException("User not found");
    }

    /**
     * Find a group based on its name
     *
     * @param groupName: the group name
     * @return the corresponding group
     * @throws IllegalStateException if a database exception occurred or no group is found
     */
    private Group findGroup(String groupName) throws SQLException {
        Group group = this.groupService.findByName(this.context, groupName);
        if (group == null) {
            throw new IllegalStateException("Group not found");
        }
        return group;
    }
}