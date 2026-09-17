/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul.script;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.pul.OnixRecordReader;
import org.dspace.uclouvain.pul.Outcome;
import org.dspace.uclouvain.pul.Outcome.Decision;
import org.dspace.uclouvain.pul.PulImportReportEmail;
import org.dspace.uclouvain.pul.PulImporter;
import org.dspace.uclouvain.pul.UpdatePolicy;
import org.dspace.util.UUIDUtils;
import org.dspace.utils.DSpace;

/**
 * Import of the PUL (Presses universitaires de Louvain) book catalogue from a directory of ONIX 3.0 files.
 * <p>
 * Each {@code <GCOI>.xml} file is turned into DIM by the {@code ONIX} crosswalk and matched against the existing
 * publications (GCOI first, then ISBN). A new book is created in the configured collection; a known one is
 * completed. Handled files go to {@code done/}, failed ones to {@code errors/}; ambiguous ones stay.
 * A report is logged and, when recipients are configured, emailed.
 * <p>
 * Run daily by a cron job; requires an administrator ({@code -e}) so that withdrawn and non-discoverable
 * publications are seen too.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class PulImport extends DSpaceRunnable<PulImportScriptConfiguration<PulImport>> {

    public static final String DIRECTORY_PROPERTY = "pul.import.directory";
    public static final String COLLECTION_PROPERTY = "pul.import.collection";
    static final String DONE_DIRECTORY = "done";
    static final String ERRORS_DIRECTORY = "errors";

    private File directory;
    private String collectionId;
    private boolean dryRun;
    private PulImporter importer;

    @Override
    public void setup() throws ParseException {
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String path = StringUtils.defaultIfBlank(commandLine.getOptionValue('d'),
            configurationService.getProperty(DIRECTORY_PROPERTY));
        if (StringUtils.isBlank(path)) {
            throw new ParseException("No directory: give --dir or set " + DIRECTORY_PROPERTY);
        }
        directory = new File(path);
        if (!directory.isDirectory()) {
            throw new ParseException("Not a directory: " + directory);
        }
        dryRun = commandLine.hasOption('n');
        collectionId = StringUtils.defaultIfBlank(commandLine.getOptionValue('c'),
            configurationService.getProperty(COLLECTION_PROPERTY));
        if (!dryRun && StringUtils.isBlank(collectionId)) {
            throw new ParseException("No target collection: give --collection or set " + COLLECTION_PROPERTY);
        }
        importer = new PulImporter(new OnixRecordReader(configurationService),
            UCLouvainServiceFactory.getInstance().getPublicationService(),
            UpdatePolicy.fromConfiguration(configurationService));
    }

    @Override
    public void internalRun() throws Exception {
        Context context = dryRun ? new Context(Context.Mode.READ_ONLY) : new Context();
        try {
            assignCurrentUser(context);
            Collection collection = dryRun ? null : resolveCollection(context);
            handler.logInfo("%s on %s (user: %s%s)".formatted(dryRun ? "Dry run" : "Import", directory,
                describeUser(context), collection == null ? "" : ", collection: " + collection.getName()));

            Map<Decision, Integer> counters = new EnumMap<>(Decision.class);
            List<String> lines = new ArrayList<>();
            for (File file : onixFiles()) {
                Outcome outcome = importer.decide(context, file);
                if (!dryRun) {
                    outcome = apply(context, outcome, collection);
                    archive(outcome);
                }
                counters.merge(outcome.decision(), 1, Integer::sum);
                lines.add(outcome.describe());
                handler.logInfo(outcome.describe());
            }
            handler.logInfo("Done: " + counters);
            report(counters, lines);
        } finally {
            context.abort();
        }
    }

    /** Write what the decision asks for, one transaction per file; a failure is reported and rolled back. */
    private Outcome apply(Context context, Outcome outcome, Collection collection) throws Exception {
        try {
            Outcome applied = switch (outcome.decision()) {
                case CREATE -> importer.create(context, outcome, collection);
                case UPDATE -> importer.update(context, outcome);
                default -> outcome;
            };
            context.commit();
            return applied;
        } catch (Exception e) {
            context.rollback();
            handler.logError("Failed on " + outcome.file().getName(), e);
            return outcome.failed(outcome.decision().name().toLowerCase() + " failed: " + e.getMessage());
        }
    }

    /** Handled files leave the directory; an ambiguous one stays for a human to look at. */
    private void archive(Outcome outcome) throws IOException {
        switch (outcome.decision()) {
            case CREATE, UPDATE, DELETED_NOTICE -> moveToSubdirectory(outcome.file(), DONE_DIRECTORY);
            case ERROR -> moveToSubdirectory(outcome.file(), ERRORS_DIRECTORY);
            default -> { }
        }
    }

    /** Move a processed file into a sibling subdirectory, replacing any older copy. */
    public static Path moveToSubdirectory(File file, String subdirectory) throws IOException {
        Path target = file.toPath().resolveSibling(subdirectory).resolve(file.getName());
        Files.createDirectories(target.getParent());
        return Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
    }

    private void report(Map<Decision, Integer> counters, List<String> lines) {
        PulImportReportEmail email = new PulImportReportEmail();
        if (email.recipients().isEmpty()) {
            handler.logInfo("No " + PulImportReportEmail.RECIPIENTS_PROPERTY + " configured: report not emailed");
            return;
        }
        try {
            email.send(directory.getPath(), dryRun, counters.toString(), lines);
        } catch (Exception e) {
            handler.logError("Report email not sent", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public PulImportScriptConfiguration<PulImport> getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("pul-import", PulImportScriptConfiguration.class);
    }

    /** The {@code .xml} files of the directory, by name; anything else is reported and skipped. */
    private List<File> onixFiles() {
        File[] entries = directory.listFiles();
        Arrays.sort(entries, Comparator.comparing(File::getName));
        for (File entry : entries) {
            if (entry.isFile() && !entry.getName().endsWith(".xml")) {
                handler.logWarning("Skipped, not an .xml file: " + entry.getName());
            }
        }
        return Arrays.stream(entries).filter(entry -> entry.isFile() && entry.getName().endsWith(".xml")).toList();
    }

    /** The target collection, by UUID or handle. */
    private Collection resolveCollection(Context context) throws Exception {
        DSpaceObject found = UUIDUtils.fromString(collectionId) != null
            ? ContentServiceFactory.getInstance().getCollectionService().find(context, UUID.fromString(collectionId))
            : HandleServiceFactory.getInstance().getHandleService().resolveToObject(context, collectionId);
        if (!(found instanceof Collection collection)) {
            throw new IllegalArgumentException("No collection with UUID or handle " + collectionId);
        }
        return collection;
    }

    /**
     * Give the context the user running the script. Through REST the framework provides its UUID; the command-line
     * variant resolves the {@code -e} email instead.
     */
    protected void assignCurrentUser(Context context) throws Exception {
        UUID epersonId = getEpersonIdentifier();
        if (epersonId != null) {
            context.setCurrentUser(EPersonServiceFactory.getInstance().getEPersonService().find(context, epersonId));
        }
    }

    private static String describeUser(Context context) {
        return context.getCurrentUser() == null ? "anonymous" : context.getCurrentUser().getEmail();
    }
}
