package org.dspace.uclouvain.administer;

import org.apache.commons.cli.*;
import org.dspace.core.Context;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract class representing a basic DSpace CLI command. This class provides
 * some basic methods for easier CLI commands creations.
 */
public abstract class AbstractCLICommand {

    protected static final Option OPT_HELP = Option.builder("h")
            .longOpt("help")
            .hasArg(false)
            .desc("explain metadata management options")
            .build();

    protected Context context = new Context();
    protected Options serviceOptions = new Options();
    protected Options infoOptions = new Options();

    protected AbstractCLICommand() {
        this.buildOptions();
    }

    /**
     * Check command line arguments passed to the CLI and check if there are
     * correct regarding CLI options.
     *
     * @param args: the list of command line argument.
     * @return the parsed command line arguments available through ``org.apache.commons.cli.CommandLine`` object.
     */
    protected CommandLine validateCLIArgument(String[] args) {
        CommandLineParser parser = new DefaultParser();
        CommandLine cl = null;
        try {
            if (infoOptions != null) {
                cl = parser.parse(infoOptions, args, true);
                if (cl.hasOption("help")) throw new ParseException("");
            }
            cl = parser.parse(serviceOptions, args, true);
            this.extraValidationCLIArgument(cl);
        } catch (ParseException pe) {
            if (!pe.getMessage().isEmpty()) System.err.println(pe.getMessage());
            usage(Arrays.asList(infoOptions, serviceOptions));
            System.exit(1);
        }
        return cl;
    }

    /**
     * This methods could be overridden by subclasses to create additional
     * validation on command line argument. If an error is detected,
     * a `ParseException` must be raised.
     *
     * @param cl: The parsed command line (containing arguments)
     * @throws ParseException If a validation error is detected.
     */
    protected void extraValidationCLIArgument(CommandLine cl) throws ParseException {
    }

    /**
     * Send the CLI command usage message to the default stream
     * @param optionsGroupList: The option groups used for the CLI command
     */
    protected void usage(List<Options> optionsGroupList) {
        UtilityCLITool.usage(
          this.getClass(),
          this.getUsageDescription(),
          optionsGroupList
        );
    }

    protected abstract String getUsageDescription();
    protected abstract void buildOptions();
}
