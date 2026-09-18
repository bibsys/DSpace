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
 * Import of the PUL (Presses universitaires de Louvain) book catalogue into DIAL.pr.
 *
 * WHAT IT DOES
 * ===============================================================
 * 1. Reads every <GCOI>.xml ONIX 3.0 file of the ONIX directory, turns it into DIM with the ONIX crosswalk
 *    (crosswalk.submission.ONIX.stylesheet) and looks for the publication carrying its GCOI, then its ISBNs (print
 *    and e-book alike, compared in normalized form):
 *      - none: a new book is created in the target collection, installed directly (no workflow);
 *      - one: it is completed according to the pul.import.update.* lists (authors are appended by missing name,
 *        existing ones are never touched);
 *      - GCOI and ISBN on different publications: nothing is done, the file stays for a human;
 *      - ONIX notification type 05 (deleted): reported only.
 *    In both create and update cases the ONIX file is kept as an administrative bitstream (METADATA bundle) and the
 *    front cover is downloaded from pul.uclouvain.be into the THUMBNAIL bundle as <GCOI>.pdf.jpg.
 * 2. Then reads every PDF of the PDF directory, if one is configured, named <GCOI>.pdf or <ISBN>.pdf, and attaches
 *    it to the publication carrying that identifier, stored as <GCOI>.pdf whatever the delivered name (ORIGINAL
 *    bundle, replacing a file of that name): open access when the publication has
 *    a dcterms.license (from the ONIX EpubLicense), restricted to the UCLouvain network otherwise. A PDF whose
 *    publication is not imported yet stays in place for a later run.
 *
 * Each file is one transaction. Handled files are moved to done/, failed ones to errors/ (sibling sub-directories);
 * ambiguous ONIX and pending PDFs stay. One report line per file is logged, and emailed to
 * pul.import.report.recipients when set.
 *
 * USAGE
 * ===============================================================
 *   dspace pul-import -e <admin email> [-d <onix dir>] [-p <pdf dir>] [-c <collection>] [-n]
 *
 *   -e, --eperson     (CLI only, required) email of the administrator running the import. An administrator is
 *                     required: the lookup must see withdrawn and non-discoverable publications, and the created
 *                     items get this submitter.
 *   -d, --dir         directory of the ONIX files; default pul.import.directory (one of the two is required).
 *   -p, --pdf-dir     directory of the PDF files (<GCOI>.pdf or <ISBN>.pdf); default pul.import.pdf-directory;
 *                     none = PDFs are not processed.
 *   -c, --collection  UUID or handle of the collection receiving new books; default pul.import.collection;
 *                     required unless --dry-run.
 *   -n, --dry-run     read, match and report what would be done; nothing is written and no file is moved.
 *
 * Other settings (pul.cfg): pul.import.update.replace / merge / add-if-empty (fields completed on update),
 * pul.import.report.recipients / subject. The ONIX and PDF directories are filled by an external process: this
 * script never talks to the PUL FTP server.
 *
 * EXAMPLES
 * ===============================================================
 *   # daily cron job, everything from pul.cfg / local.cfg
 *   dspace pul-import -e bibsys@uclouvain.be
 *   # first look at a new delivery without writing anything
 *   dspace pul-import -e bibsys@uclouvain.be -d /data/pul/onix --dry-run
 *   # explicit directories and collection
 *   dspace pul-import -e bibsys@uclouvain.be -d /data/pul/onix -p /data/pul/pdf -c 2078.5/Publication
 *
 * Also exposed through the REST scripts endpoint as pul-import (the user is then the authenticated one).
 * Report lines look like:
 *
 *   CREATE   29303100050380.xml  "Mnemosyne..." -> item 613ae6f9-... [archived] -- ...; created 2078.5/5284
 *   UPDATE   29303100293170.xml  "Crisis to Collapse" -> item 8a31eb67-... [archived] -- matched by ISBN [...]
 *   PENDING  29303100999999.pdf -- no publication with GCOI 29303100999999 yet, left for a later run
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class PulImport extends DSpaceRunnable<PulImportScriptConfiguration<PulImport>> {

    public static final String DIRECTORY_PROPERTY = "pul.import.directory";
    public static final String COLLECTION_PROPERTY = "pul.import.collection";
    public static final String PDF_DIRECTORY_PROPERTY = "pul.import.pdf-directory";
    static final String DONE_DIRECTORY = "done";
    static final String ERRORS_DIRECTORY = "errors";

    private File directory;
    private File pdfDirectory;
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
        String pdfPath = StringUtils.defaultIfBlank(commandLine.getOptionValue('p'),
            configurationService.getProperty(PDF_DIRECTORY_PROPERTY));
        if (StringUtils.isNotBlank(pdfPath)) {
            pdfDirectory = new File(pdfPath);
            if (!pdfDirectory.isDirectory()) {
                throw new ParseException("Not a directory: " + pdfDirectory);
            }
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
            for (File file : filesOf(directory, ".xml")) {
                Outcome outcome = importer.decide(context, file);
                if (!dryRun) {
                    outcome = apply(context, outcome, collection);
                    archive(outcome);
                }
                record(outcome, counters, lines);
            }
            if (pdfDirectory != null) {
                handler.logInfo("PDF files of " + pdfDirectory);
                for (File pdf : filesOf(pdfDirectory, ".pdf")) {
                    Outcome outcome = importer.decidePdf(context, pdf);
                    if (!dryRun) {
                        outcome = applyPdf(context, outcome);
                        archive(outcome);
                    }
                    record(outcome, counters, lines);
                }
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

    private Outcome applyPdf(Context context, Outcome outcome) throws Exception {
        if (outcome.decision() != Decision.UPDATE) {
            return outcome;
        }
        try {
            Outcome applied = importer.storePdf(context, outcome);
            context.commit();
            return applied;
        } catch (Exception e) {
            context.rollback();
            handler.logError("Failed on " + outcome.file().getName(), e);
            return outcome.failed("pdf failed: " + e.getMessage());
        }
    }

    private void record(Outcome outcome, Map<Decision, Integer> counters, List<String> lines) {
        counters.merge(outcome.decision(), 1, Integer::sum);
        lines.add(outcome.describe());
        handler.logInfo(outcome.describe());
    }

    /** Handled files leave the directory; an ambiguous ONIX or a PDF without publication yet stays. */
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

    /** The files of the directory with the given extension, by name; anything else is reported and skipped. */
    private List<File> filesOf(File dir, String extension) {
        File[] entries = dir.listFiles();
        Arrays.sort(entries, Comparator.comparing(File::getName));
        for (File entry : entries) {
            if (entry.isFile() && !entry.getName().toLowerCase().endsWith(extension)) {
                handler.logWarning("Skipped, not a %s file: %s".formatted(extension, entry.getName()));
            }
        }
        return Arrays.stream(entries)
            .filter(entry -> entry.isFile() && entry.getName().toLowerCase().endsWith(extension))
            .toList();
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
     * variant resolves the -e email instead.
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
