package org.dspace.administer;

import org.apache.commons.cli.*;
import org.dspace.core.Context;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract class representing a basic DSpace CLI command. This class provides
 * some basic methods for easier CLI commands creations.
 */
public abstract class AbstractCLICommand {

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
        } catch (ParseException pe) {
            if (!pe.getMessage().isEmpty()) System.err.println(pe.getMessage());
            usage(Arrays.asList(infoOptions, serviceOptions));
            System.exit(1);
        }
        return cl;
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
