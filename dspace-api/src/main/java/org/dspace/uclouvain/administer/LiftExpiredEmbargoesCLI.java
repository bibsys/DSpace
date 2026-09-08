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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

/**
 * A command-line tool turning expired embargoes into open access policies.
 *
 * An embargo is an anonymous READ policy named "embargo" whose start date is in the future. Once that date is
 * reached the policy grants access, but nothing tells the item: its access status metadata and its Solr document
 * still say "embargo". Renaming the policy fires a MODIFY event on the bitstream, which drives both updates.
 *
 * This is wired as the {@code embargo-lifter} command in {@code launcher.xml}, replacing the upstream
 * {@code org.dspace.embargo.EmbargoCLITool}, which only handles the metadata-based embargoes of DSpace 1.x/3.x
 * ({@code embargo.field.terms} / {@code embargo.field.lift}) and never touches policies.
 *
 * USAGE:
 *   dspace embargo-lifter [-n]
 *
 * ARGUMENTS:
 *   -n, --dry-run:  list the embargoes that would be lifted without changing anything
 *   -h, --help:     display the options
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class LiftExpiredEmbargoesCLI extends AbstractCLICommand {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public static final String USAGE_DESCRIPTION = "A command-line tool to turn expired embargoes into open access";

    private static final Option OPT_DRY_RUN = Option.builder("n")
        .longOpt("dry-run")
        .hasArg(false)
        .desc("list the embargoes that would be lifted without changing anything")
        .build();

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final UCLouvainResourcePolicyService resourcePolicyService;

    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected LiftExpiredEmbargoesCLI() {
        resourcePolicyService = UCLouvainServiceFactory.getInstance().getResourcePolicyService();
    }

    public static void main(String[] argv) throws Exception {
        LiftExpiredEmbargoesCLI cli = new LiftExpiredEmbargoesCLI();
        CommandLine cl = cli.validateCLIArgument(argv);
        try (Context session = cli.context) {
            cli.run(session, cl.hasOption(OPT_DRY_RUN));
            session.complete();
        }
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_DRY_RUN);
        infoOptions.addOption(OPT_HELP);
    }

    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    /**
     * Lift every expired embargo found in the database, printing one line per policy.
     * The caller owns the context: nothing is committed here.
     *
     * @param context the DSpace context
     * @param dryRun  when true, only print what would be done
     * @return the number of embargoes lifted (or that would have been lifted)
     */
    protected int run(Context context, boolean dryRun) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        List<ResourcePolicy> expired = resourcePolicyService.findExpiredEmbargoes(context);
        for (ResourcePolicy policy : expired) {
            DSpaceObject dso = policy.getdSpaceObject();
            System.out.println((dryRun ? "DRY RUN: would lift" : "Lifting") + " embargo expired on "
                + policy.getStartDate() + " for " + Constants.typeText[dso.getType()].toLowerCase()
                + " " + dso.getID() + " (" + dso.getName() + ")");
            if (!dryRun) {
                resourcePolicyService.liftEmbargo(context, policy);
            }
        }
        System.out.println(expired.size() + " expired embargo(es) found.");
        return expired.size();
    }
}
