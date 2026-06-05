/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.script;

import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.DSpaceCommandLineParameter;
import org.dspace.scripts.configuration.ScriptConfiguration;

public class PublicationExportScriptConfiguration<T extends PublicationExport> extends ScriptConfiguration<T> {
    private Class<T> dspaceRunnableClass;

    @Override
    public boolean isAllowedToExecute(Context context, List<DSpaceCommandLineParameter> commandLineParameters) {
        // Make sure the user is authenticated to allow bibliographic export.
        return context.getCurrentUser() != null;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            options = new Options();

            options.addOption(
                Option.builder("u")
                    .longOpt("uuid")
                    .desc("The uuid of the profile to export publication of")
                    .hasArg(true)
                    .type(String.class)
                    .required(false)
                    .build()
            );
            options.addOption(
                Option.builder("f")
                    .longOpt("fgs")
                    .desc("The fgs of the profile to export publication of")
                    .hasArg(true)
                    .type(String.class)
                    .required(false)
                    .build()
            );
            options.addOption(
                Option.builder("t")
                    .longOpt("type")
                    .desc("The type of export to perform: 'fnrs' or 'fwb'")
                    .hasArg(true)
                    .type(String.class)
                    .required(true)
                    .build()
            );
            options.addOption(
                Option.builder("s")
                    .longOpt("startYear")
                    .desc("Export only publications after this date")
                    .hasArg(true)
                    .type(String.class)
                    .required(false)
                    .build()
            );
            options.addOption(
                Option.builder("e")
                    .longOpt("endYear")
                    .desc("Export only publications before this date")
                    .hasArg(true)
                    .type(String.class)
                    .required(false)
                    .build()
            );
            options.addOption(
                Option.builder("i")
                    .longOpt("includePosters")
                    .desc("Include posters into the export. Default is false.")
                    .hasArg(true)
                    .required(false)
                    .build()
            );
        }
        return options;
    }

    // SETTER && GETTER
    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }
}
