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
import java.io.InputStream;
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
 *   dspace dsrun org.dspace.uclouvain.administer.ImportVersioning -e [mail] -f [path_file]
 *
 * ARGUMENTS:
 *   -e:       the email of the ePerson that will ingest the archive.
 *   -f:       Path to JSON file.
 *
 * @author Ayoub Chaalane <ayoub.chaalane@uclouvain.be>
 * @version $Revision$
 */
public class ImportVersioning extends AbstractCLICommand {

    private static final Logger logger = LoggerFactory.getLogger(ImportVersioning.class);

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
        try {
            context.turnOffAuthorisationSystem();
            setupEPersonContext(context, epersonEmail);

            JsonNode root = readJsonFile(fileString);
            logger.info("Starting version linking from JSON File: {}", fileString);

            Map<String, List<JsonNode>> grouped = groupByRequestedUuid(root);
            processGroups(context, grouped);

            logger.info("ImportVersioning completed.");
        } finally {
            if (context.isValid()) {
                context.complete();
            }
            context.restoreAuthSystemState();
        }
    }

    private void setupEPersonContext(Context context, String epersonEmail) throws UserNotFoundException, SQLException {
        EPerson ePerson = Optional
            .ofNullable(epersonService.findByEmail(context, epersonEmail))
            .orElseThrow(() -> new UserNotFoundException("User not found: " + epersonEmail));
        context.setCurrentUser(ePerson);
    }

    private JsonNode readJsonFile(String fileString) throws IOException {
        try (InputStream is = new FileInputStream(new File(fileString))) {
            return new ObjectMapper().readTree(is);
        }
    }

    private void processGroups(Context context, Map<String, List<JsonNode>> grouped) throws Exception {
        for (Map.Entry<String, List<JsonNode>> entry : grouped.entrySet()) {
            String requestedUuid = entry.getKey();
            List<JsonNode> versions = entry.getValue();

            logger.info("Processing parent item group with requested_uuid: {}", requestedUuid);

            Item parentItem = findItemByIdentifierOer(context, requestedUuid);
            if (parentItem == null) {
                logger.error("Parent Item not found in DSpace for legacyOER: {}", requestedUuid);
                continue;
            }

            try {
                processVersionGroup(context, requestedUuid, parentItem, versions);
                context.commit();
            } catch (Exception e) {
                context.rollback();
                logger.error("Error committing versioning transaction for group {}", requestedUuid, e);
            }
        }
    }

    private void processVersionGroup(
        Context context,
        String requestedUuid,
        Item parentItem,
        List<JsonNode> versions
    ) throws Exception {
        versions.sort(Comparator.comparingInt(v -> v.get("version_number").asInt()));
        VersionHistory vh = resolveOrCreateVersionHistory(context, parentItem, versions, requestedUuid);

        for (JsonNode node : versions) {
            processVersionNode(context, vh, node);
        }
    }

    private VersionHistory resolveOrCreateVersionHistory(
        Context context,
        Item parentItem,
        List<JsonNode> versions,
        String requestedUuid
    ) throws Exception {
        VersionHistory vh = vsHistoryService.findByItem(context, parentItem);
        if (vh != null) {
            return vh;
        }

        // Try finding existing VersionHistory from child candidates
        for (JsonNode vNode : versions) {
            String childUuid = vNode.get("item_id").asText();
            Item childCandidate = findItemByIdentifierOer(context, childUuid);
            if (childCandidate != null) {
                vh = vsHistoryService.findByItem(context, childCandidate);
                if (vh != null) {
                    return vh;
                }
            }
        }

        logger.info("Created new VersionHistory for requested_uuid: {}", requestedUuid);
        return vsHistoryService.create(context);
    }

    private void processVersionNode(Context context, VersionHistory vh, JsonNode node) throws Exception {
        String itemUUID = node.get("item_id").asText();
        Item itemChild = findItemByIdentifierOer(context, itemUUID);
        if (itemChild == null) {
            logger.error("Child Item not found for legacyOER: {}", itemUUID);
            return;
        }

        int versionNumber = node.get("version_number").asInt();
        Date versionDate = new DCDate(node.get("version_date").asText()).toDate();
        String summary = node.has("version_summary") ? node.get("version_summary").asText() : "";

        Version currentVersion = versioningService.getVersion(context, itemChild);
        if (currentVersion == null) {
            versioningService.createNewVersion(context, vh, itemChild, summary, versionDate, versionNumber);
        } else {
            updateExistingVersion(context, currentVersion, vh, summary, versionDate, versionNumber);
        }

        itemChild.setArchived(node.get("in_archive").asBoolean());
        itemChild.setDiscoverable(node.get("discoverable").asBoolean());
        itemService.update(context, itemChild);

        logger.info("Successfully attached item {} as Version {} in VH {}", itemUUID, versionNumber, vh.getID());
    }

    private void updateExistingVersion(
        Context context,
        Version version,
        VersionHistory targetVh,
        String summary,
        Date versionDate,
        int versionNumber
    ) throws Exception {
        VersionHistory oldVh = version.getVersionHistory();
        if (oldVh != null && !oldVh.getID().equals(targetVh.getID())) {
            vsHistoryService.remove(oldVh, version);
            try {
                vsHistoryService.delete(context, oldVh);
            } catch (Exception e) {
                logger.error("Failed to delete VersionHistory ID: {}", oldVh.getID(), e);
            }
        }

        version.setVersionHistory(targetVh);
        version.setSummary(summary);
        version.setVersionDate(versionDate);
        version.setVersionNumber(versionNumber);
        versioningService.update(context, version);
    }

    private Item findItemByIdentifierOer(
        Context context, String uuid
    ) throws SQLException, IOException, AuthorizeException {
        Iterator<Item> it =
            itemService.findUnfilteredByMetadataField(
                context, "dc", "identifier", "legacyOER", uuid
            );
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