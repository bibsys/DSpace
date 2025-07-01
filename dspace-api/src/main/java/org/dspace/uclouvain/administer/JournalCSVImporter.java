/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.administer;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.FileReader;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvToBeanBuilder;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageUtils;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.journals.Journal;
import org.dspace.uclouvain.journals.JournalService;

/**
 * Import journals into DSpace using a CSV file as input.
 * The input file has the following columns (order matters):
 * Title | ISSN | eISSN | Publisher | Publisher location (city) | Peer-reviewed | Status Code
 * 
 * First the script loads the data from the input file then it performs a search in Solr to list all the current ISSN.
 * We create a new journal for each issn that is not yet present in the system.
 * TODO: We should have a 'update' mode that could allow to update the already present journal objects.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class JournalCSVImporter extends AbstractCLICommand {
    private static final Option OPT_PERSON = Option.builder("e")
        .longOpt("eperson")
        .hasArg(true)
        .desc("The email address of eperson doing the import.")
        .required(true)
        .build();
    private static final Option OPT_FILE_PATH = Option.builder("f")
        .longOpt("file")
        .hasArg(true)
        .desc("The path of the .CSV file containing data to import.")
        .required(true)
        .build();
    private static final Option OPT_PARENT_COLLECTION_UUID = Option.builder("c")
        .longOpt("collection_UUID")
        .hasArg(true)
        .desc("The uuid of the collection to create the journals in.")
        .required(true)
        .build();
    public static final String USAGE_DESCRIPTION = "A command-line tool to import journals from a CSV file.";

    private final Context context;
    private static final EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static final WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance()
        .getWorkspaceItemService();
    private static final JournalService journalService = UCLouvainServiceFactory.getInstance().getJournalService();
    private static final CollectionService collectionService =
        ContentServiceFactory.getInstance().getCollectionService();

    /**
     * Prepare the script and run it for a CLI execution.
     * @param argv The arguments given to the command line.
     * @throws Exception if any error occurred while running the import script.
     */
    public static void main(String[] argv) throws Exception {
        JournalCSVImporter journalImporter = new JournalCSVImporter();
        CommandLine cli = journalImporter.validateCLIArgument(argv);

        journalImporter.run(cli.getOptionValue("e"), cli.getOptionValue("f"), cli.getOptionValue("c"));
    }

    protected JournalCSVImporter() {
        context = new Context();
    }

    @Override
    protected void buildOptions() {
        serviceOptions.addOption(OPT_PERSON);
        serviceOptions.addOption(OPT_FILE_PATH);
        serviceOptions.addOption(OPT_PARENT_COLLECTION_UUID);
        infoOptions.addOption(OPT_HELP);
    }

    @Override
    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    /**
     * Run the journal import script.
     * 
     * 1. Read the CSV file and load the data as a list of object.
     * 2. For each object, create a journal in DSpace with the corresponding metadata.
     * 3. Commit the context to persist the changes.
     * 
     * @param personEmail The email of the user to use as submitter.
     * @param filePath The path to the CSV file containing the journals to import.
     * @throws Exception If any error occurres while running the script.
     */
    private void run(String personEmail, String filePath, String collectionUUID) throws Exception {
        System.out.println("------------------------------------------------");
        System.out.println("---------------JOURNAL EXPORT CLI---------------");
        System.out.println("------------------------------------------------");

        // Get the collection to put items in.
        Collection parentCollection = collectionService.find(context, UUID.fromString(collectionUUID));

        if (parentCollection == null) {
            System.err.println("Cannot find collection with UUID " + collectionUUID);
            return;
        }

        // Load the journals data from the CSV file and parse it to a list of JournalData objects.
        List<JournalData> journalsData = new CsvToBeanBuilder<JournalData>(new FileReader(filePath))
            .withType(JournalData.class)
            .withIgnoreLeadingWhiteSpace(true)
            .build()
            .parse();

        System.out.println("Number of loaded journals to import: " + journalsData.size());

        context.turnOffAuthorisationSystem();
        // Get the eperson that will be used as the submitter of the journals.
        EPerson person = epersonService.findByEmail(context, personEmail);
        if (person == null) {
            System.out.println(String.format("Could not find any person matching: '%s', aborting...", personEmail));
            return;
        }
        context.setCurrentUser(person);

        for (JournalData journal : journalsData) {
            Journal existingJournal = journalService.findByIssn(context, journal.issn);
            if (existingJournal != null) {
                System.out.println("Journal ISSN '" + journal.issn + "' already existing, skipping creation...");
                continue;
            }
            createEntity(parentCollection, journal);
        }
        context.complete();
        context.restoreAuthSystemState();
        System.out.println("Journal import script done !");
    }

    /**
     * Create a Journal item for the given data in a given collection
     * @param collection The collection to create the item in.
     * @param journalData The data to add to the item once created.
     * @throws Exception
     */
    private void createEntity(Collection collection, JournalData journalData) throws Exception {
        // Create an entity from the journal data.
        PackageParameters params = new PackageParameters();
        // Make sure that the item is archive directly.
        params.setWorkflowEnabled(false);

        Item item = (Item) PackageUtils.createDSpaceObject(context, collection, Constants.ITEM, null, null, params);
        // Enrich item metadata based on extracted data.
        addMetadata(item, journalData);
        // Make sure that the item is not in workspace state, if it is: deposit it.
        WorkspaceItem wsi = workspaceItemService.findByItem(context, item);
        if (wsi != null) {
            PackageUtils.finishCreateItem(context, wsi, null, params);
        }
        // Update the object to apply the changes.
        PackageUtils.updateDSpaceObject(context, item);
        // Commit the changes in database.
        context.commit();
        System.out.println("Created a journal for issn " + journalData.issn);
    }

    /**
     * Add the required metadata to a freshly created journal.
     * @param item The item to add metadata to.
     * @param journalData The object containing the data to add.
     * @throws SQLException If any error occurred when adding a metadata.
     */
    private void addMetadata(Item item, JournalData journalData) throws SQLException {
        itemService.addMetadata(context, item, "dc", "title", null, null, journalData.title);
        if (isNotEmpty(journalData.issn)) {
            itemService.addMetadata(context, item, "dc", "identifier", "issn", null, journalData.issn);
        }
        if (isNotEmpty(journalData.eissn)) {
            itemService.addMetadata(context, item, "dc", "identifier", "eissn", null, journalData.eissn);
        }
        itemService.addMetadata(context, item, "dc", "publisher", null, null, journalData.publisher);
        if (isNotEmpty(journalData.publisherLocation)) {
            itemService.addMetadata(context, item, "dc", "publisher", "location", null, journalData.publisherLocation);
        }
        itemService.addMetadata(context, item, "journal", "peerReviewed", null, null, journalData.peerReviewed + "");
        itemService.addMetadata(context, item, "journal", "statusCode", null, null, journalData.statusCode);
    }

    /**
     * Class used for the mapping of journal data.
     * Each data has to be put in the right order in the CSV file.
     */
    public static class JournalData {
        @CsvBindByPosition(position = 0)
        private String title;
        @CsvBindByPosition(position = 1)
        private String issn;
        @CsvBindByPosition(position = 2)
        private String eissn;
        @CsvBindByPosition(position = 3)
        private String publisher;
        @CsvBindByPosition(position = 4)
        private String publisherLocation;
        @CsvBindByPosition(position = 5)
        private boolean peerReviewed;
        @CsvBindByPosition(position = 6)
        private String statusCode;

        public JournalData() {}

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getIssn() {
            return issn;
        }

        public void setIssn(String issn) {
            this.issn = issn;
        }

        public String getEissn() {
            return eissn;
        }

        public void setEissn(String eissn) {
            this.eissn = eissn;
        }

        public String getPublisher() {
            return publisher;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }

        public String getPublisherLocation() {
            return publisherLocation;
        }

        public void setPublisherLocation(String publisherLocation) {
            this.publisherLocation = publisherLocation;
        }

        public boolean isPeerReviewed() {
            return peerReviewed;
        }

        public void setPeerReviewed(boolean peerReviewed) {
            this.peerReviewed = peerReviewed;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }
    }
}
