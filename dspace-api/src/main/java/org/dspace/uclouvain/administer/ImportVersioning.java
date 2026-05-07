/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.administer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.uclouvain.exceptions.UserNotFoundException;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command-line tool to link versions.
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.ImportVersioning -f [path_file]
 *
 * ARGUMENTS:
 *   -f:       Path to JSON file.
 *
 * @author Ayoub Chaalane <ayoub.chaalane@uclouvain.be>
 * @version $Revision$
 */
public class ImportVersioning extends AbstractCLICommand {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private static final Logger logger = LoggerFactory.getLogger(ImportVersioning.class);
    /** CLI available options */
    private static final Option OPT_PERSON = Option.builder("e")
            .longOpt("eperson")
            .hasArg(true)
            .desc("email address of eperson doing importing")
            .required(true)
            .build();
    private static final Option OPT_FILE = Option.builder("f")
            .longOpt("file")
            .hasArg(true)
            .desc("Path to JSON file")
            .required(true)
            .build();

     // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public static final String USAGE_DESCRIPTION = "A command-line tool for importing versioning";
    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private VersioningService versioningService = VersionServiceFactory.getInstance().getVersionService();
    private VersionHistoryService vsHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();
    private EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();

    public static void main(String[] argv) throws Exception {
        ImportVersioning versioning = new ImportVersioning();
        CommandLine cl = versioning.validateCLIArgument(argv);

        try {
            versioning.run(cl.getOptionValue('e'), cl.getOptionValue('f'));
        } catch (Exception ex) {
            logger.error(ex.getClass().getName() + " :: " + ex.getMessage(), ex);
        }
    }

    private void run(String epersonEmail, String fileString) throws Exception {
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        EPerson ePerson = Optional.ofNullable(epersonService.findByEmail(context, epersonEmail))
            .orElseThrow(() -> new UserNotFoundException("Not found User : " + epersonEmail));
        context.setCurrentUser(ePerson);
        JsonNode root = new ObjectMapper().readTree(new FileInputStream(new File(fileString)));

        logger.info("Starting version linking from JSON File: " + fileString);

        Map<String, List<JsonNode>> grouped = groupByRequestedUuid(root);

        for (String requestedUuid : grouped.keySet()) {
            logger.info("Processing item: " + requestedUuid);
            Item item = findItemByIdentifierOer(context, requestedUuid);
            if (item == null) {
                logger.error("Item not found in DSpace: " + requestedUuid);
                continue;
            }

            VersionHistory vh = vsHistoryService.findByItem(context, item);
            if (vh == null) {
                vh = vsHistoryService.create(context);
                logger.info("Created new VersionHistory for item " + requestedUuid);
            }

            List<JsonNode> versions = grouped.get(requestedUuid);
            versions.sort(Comparator.comparingInt(v -> v.get("version_number").asInt()));

            for (JsonNode node : versions) {
                int versionNumber = node.get("version_number").asInt();
                String versionDateString = node.get("version_date").asText();
                String summary = node.get("version_summary").asText();
                String itemUUID = node.get("item_id").asText();
                boolean inArchive = node.get("in_archive").asBoolean();
                boolean discoverable = node.get("discoverable").asBoolean();

                Item itemChild = findItemByIdentifierOer(context, itemUUID);
                if (itemChild == null) {
                    logger.error("Item not found in DSpace: " + itemUUID);
                    continue;
                }

                logger.info("Linking version " + versionNumber + " for item " + itemUUID);

                Version v = versioningService.getVersion(context, itemChild);
                if (v != null) {
                    continue;
                }

                Date versionDate = new DCDate(versionDateString).toDate();
                v = versioningService.createNewVersion(context, vh, itemChild, summary, versionDate, versionNumber);

                itemChild.setArchived(inArchive);
                itemChild.setDiscoverable(discoverable);
                itemService.update(context, itemChild);

                logger.info("Linked version " + versionNumber + " to item " + requestedUuid);
            }
        }

        context.complete();
        context.restoreAuthSystemState();
        logger.info("Version linking completed.");
    }

    private Item findItemByIdentifierOer(
        Context context, String uuid
    ) throws SQLException, IOException, AuthorizeException {
        Iterator<Item> it =
            itemService.findByMetadataField(context, "dc", "identifier", "legacyOER", uuid);
        if (!it.hasNext()) {
            return null;
        }
        Item first = it.next();
        if (it.hasNext()) {
            throw new SQLException("Multiple items found for identifier legacyOER: " + uuid);
        }
        return first;
    }

    private Map<String, List<JsonNode>> groupByRequestedUuid(JsonNode root) {
        return StreamSupport.stream(root.spliterator(), false)
                .collect(Collectors.groupingBy(node -> node.path("requested_uuid").asText()));
    }

    @Override
    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }

    @Override
    protected void buildOptions() {
        serviceOptions.addOption(OPT_PERSON);
        serviceOptions.addOption(OPT_FILE);
        infoOptions.addOption(OPT_HELP);
    }
}
