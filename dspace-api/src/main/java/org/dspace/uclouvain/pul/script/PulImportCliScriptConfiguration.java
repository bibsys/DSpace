/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul.script;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Command-line variant of {@link PulImportScriptConfiguration}: adds the mandatory {@code -e} user email.
 *
 * @param <T> the runnable
 */
public class PulImportCliScriptConfiguration<T extends PulImportCli> extends PulImportScriptConfiguration<T> {

    @Override
    public Options getOptions() {
        Options options = super.getOptions();
        if (options.getOption("e") == null) {
            options.addOption(Option.builder("e").longOpt("eperson")
                .desc("Email of the administrator running the import")
                .hasArg(true).required(true).build());
        }
        return options;
    }
}
