package org.dspace.administer;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.List;

public class UtilityCLITool {

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
