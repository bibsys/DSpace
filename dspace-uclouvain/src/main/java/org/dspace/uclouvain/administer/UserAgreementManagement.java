package org.dspace.uclouvain.administer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

import java.sql.SQLException;

/**
 * A command-line tool for managing the "user agreement" validation of an ePerson.
 * User can choose to automatically validated the user agreement licence; or to disable it (if user has already
 * validated the licence).
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.UserAgreementManagement  -u [user] --enable|disable
 *
 * ARGUMENTS:
 *   -u, --user: the user email or user UUID (aka NetID)
 *   --enable  : validate the user agreement
 *   --disable : disable the user agreement (Next login attempt, the user should accept again the licence)
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 * @version $Revision$
 */
public class UserAgreementManagement extends AbstractCLICommand {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * CLI available options
     */
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
    private static final Option OPT_ENABLE = Option.builder("e")
            .longOpt("enable")
            .hasArg(false)
            .desc("Validate the user agreement")
            .build();
    private static final Option OPT_DISABLE = Option.builder("d")
            .longOpt("disable")
            .hasArg(false)
            .desc("Invalidate the user agreement")
            .build();

    public static final String USAGE_DESCRIPTION = "A command-line tool to automatically manage the user agreement for a user";

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final Context context = new Context();
    protected static EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * For invoking via the command line. If called with no command line arguments,
     * it will negotiate with the user for the administrator details
     *
     * @param argv: the command line arguments given
     * @throws MissingArgumentException : If a required argument is missing.
     */
    public static void main(String[] argv) throws Exception {
        UserAgreementManagement uam = new UserAgreementManagement();
        CommandLine cl = uam.validateCLIArgument(argv);
        uam.manageUserAgreement(cl.getOptionValue("u"), cl.hasOption("enable"));
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_USER);
        serviceOptions.addOption(OPT_ENABLE);
        serviceOptions.addOption(OPT_DISABLE);
        infoOptions.addOption(OPT_HELP);
    }

    protected String getUsageDescription() {
        return UserGroupManagement.USAGE_DESCRIPTION;
    }

    protected void extraValidationCLIArgument(CommandLine cl) throws ParseException {
        if (!cl.hasOption("enable") && !cl.hasOption("disable")) {
            throw new ParseException("'enable' or 'disable' argument is required");
        }
    }

    // PRIVATE FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Perform user agreement validation/invalidation
     *
     * @param userID: the user's identifier use to retrieve the ePerson.
     * @param validation: if true, the user agreement must be validated, otherwise user agreement will be disabled
     * @throws SQLException if any exceptions occurred during process.
     * @throws AuthorizeException if any authorization exceptions occurred during process.
     */
    private void manageUserAgreement(String userID, boolean validation) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        EPerson eperson = UtilityCLITool.findUser(context, userID);
        if (validation) {
            ePersonService.setMetadataSingleValue(context, eperson, "dspace", "agreements", "end-user", null, "true");
        } else {
            ePersonService.clearMetadata(context, eperson, "dspace", "agreements", "end-user", null);
        }
        ePersonService.update(context, eperson);
        context.complete();
    }
}
