package org.dspace.uclouvain.administer;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;

import java.sql.SQLException;
import java.util.List;

public class UtilityCLITool {

    /**
     * Find an ePerson based either on email, either on UUID (aka. NetID)
     *
     * @param userID: the user's identifier
     * @return the corresponding ePerson
     * @throws IllegalStateException if a database exception occurred or no ePerson is found.
     */
    public static EPerson findUser(Context ctx, String userID) throws IllegalStateException {
        EPersonService service = EPersonServiceFactory.getInstance().getEPersonService();
        if (userID == null || userID.isEmpty()) {
            throw new IllegalStateException("user ID must be specified");
        }
        try {
            EPerson eperson = service.findByEmail(ctx, userID);
            if (eperson != null) {
                return eperson;
            }
            eperson = service.findByNetid(ctx, userID);
            if (eperson != null) {
                return eperson;
            }
        } catch(SQLException e) {
            throw new IllegalStateException("User not found :: "+e.getMessage());
        }
        throw new IllegalStateException("User not found");
    }

    /**
     * Allows to combine multiple CLI options group into a single options set.
     * This is useful to format CLI command usage string.
     *
     * @param optionsGroupList: the options group to merge together.
     * @return the merged options list.
     */
    public static Options combineOptions(List<Options> optionsGroupList) {
        Options opts = new Options();
        for (Options optGroup : optionsGroupList) {
            for (Option opt : optGroup.getOptions()) {
                opts.addOption(opt);
            }
        }
        return opts;
    }

    /**
     * Display the usage of a command line with all possible arguments.
     *
     * @param cls: the CLI class to manage
     * @param description: a human-readable description for the CLI.
     * @param optionsGroupList: All options available for the CLI.
     */
    public static void usage(Class<?> cls, String description, List<Options> optionsGroupList) {
        String header = "\n" + description + "\n\n";
        String footer = "\n";
        Options options = combineOptions(optionsGroupList);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("dspace dsrun " + cls.getCanonicalName(), header, options, footer, true);
    }
}
