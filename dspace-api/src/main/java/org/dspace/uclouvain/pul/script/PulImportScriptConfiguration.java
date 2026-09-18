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
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * Options of the {@code pul-import} script. Execution is reserved to administrators (default rule of
 * {@link ScriptConfiguration}): the import must see withdrawn and non-discoverable publications.
 *
 * @param <T> the runnable
 */
public class PulImportScriptConfiguration<T extends PulImport> extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public Options getOptions() {
        if (options == null) {
            options = new Options();
            options.addOption(Option.builder("d").longOpt("dir")
                .desc("Directory holding the ONIX files (default: " + PulImport.DIRECTORY_PROPERTY + ")")
                .hasArg(true).required(false).build());
            options.addOption(Option.builder("p").longOpt("pdf-dir")
                .desc("Directory holding the <GCOI>.pdf files to attach (default: " + PulImport.PDF_DIRECTORY_PROPERTY
                    + "; none = PDFs are not processed)")
                .hasArg(true).required(false).build());
            options.addOption(Option.builder("c").longOpt("collection")
                .desc("UUID or handle of the collection receiving new publications (default: "
                    + PulImport.COLLECTION_PROPERTY + ")")
                .hasArg(true).required(false).build());
            options.addOption(Option.builder("n").longOpt("dry-run")
                .desc("Report what would be done without writing anything")
                .hasArg(false).required(false).build());
        }
        return options;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }
}
