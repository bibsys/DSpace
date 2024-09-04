package org.dspace.uclouvain.administer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageUtils;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.uclouvain.exceptions.NotUniqueResultException;

import java.io.File;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * A command-line tool to import entities from configuration file as OrgUnit DSpace item.
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.OrgUnitImporter -f [filepath] -u [email] [-t "item_type"]
 *
 * ARGUMENTS:
 *   -f, --file: file containing the structure to import
 *   -u, --user: user email address used to import item
 *   -t, --default-type: default `dc.type` to assign to the created item
 *   -c, --collection-name: The collection name where the OrgUnit item will be created
 *   -h, --help: display OrgUnitImporter CLI options.
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 * @version $Revision$
 */
public class OrgUnitImporter extends AbstractCLICommand {

    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** CLI available options */
    private static final Option OPT_NAME = Option.builder("f")
            .longOpt("file")
            .hasArg(true)
            .desc("file containing the structure to import")
            .required(true)
            .build();
    private static final Option OPT_USER = Option.builder("u")
            .longOpt("user")
            .hasArg(true)
            .desc("user email address")
            .required(true)
            .build();
    private static final Option OPT_COLLECTION = Option.builder("c")
            .longOpt("collection-name")
            .hasArg(true)
            .desc("The collection name where the OrgUnit item will be created")
            .required(true)
            .build();
    private static final Option OPT_DEFAULT_TYPE = Option.builder("t")
            .longOpt("default-type")
            .hasArg(true)
            .desc("default `dc.type` value to assign to the OrgUnit item (if not specified into config file")
            .required(false)
            .build();
    public static final String USAGE_DESCRIPTION = "A command-line tool to import an OrgUnit structure from file";

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private final Context context;
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static final WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
    private static final MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
    private static final SearchService searchService = SearchUtils.getSearchService();
    private static final EPersonService epersonService = EPersonServiceFactory.getInstance().getEPersonService();

    private String defaultEntityType;
    private String owningCollectionName;

    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * For invoking via the command line.
     *
     * @param argv: the command line arguments given
     * @throws MissingArgumentException : If a required argument is missing.
     */
    public static void main(String[] argv) throws Exception {
        OrgUnitImporter oui = new OrgUnitImporter();
        CommandLine cl = oui.validateCLIArgument(argv);
        oui.defaultEntityType = cl.getOptionValue("t", "Research Institute");
        oui.owningCollectionName = cl.getOptionValue("c");
        oui.run(cl.getOptionValue("f"), cl.getOptionValue("u"));
    }
    protected OrgUnitImporter() {
        context = new Context();
    }
    @Override
    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }
    @Override
    protected void buildOptions() {
        serviceOptions.addOption(OPT_NAME);
        serviceOptions.addOption(OPT_USER);
        serviceOptions.addOption(OPT_DEFAULT_TYPE);
        serviceOptions.addOption(OPT_COLLECTION);
        infoOptions.addOption(OPT_HELP);
    }

    // PRIVATE FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void run(String filePath, String userEmail) throws Exception {
        context.turnOffAuthorisationSystem();
        EPerson user = epersonService.findByEmail(this.context, userEmail);
        this.context.setCurrentUser(user);
        Collection parentCollection = getOrgUnitCollection();
        List<Entity> entities = readInputFile(filePath);
        for (Entity entity : entities) {
            createEntity(entity, null, parentCollection, 0);
        }
        context.complete();
        context.restoreAuthSystemState();
    }

    /**
     * Read the input JSON file containing the entities tree to create.
     * @param filePath: The full file path to read
     * @return The list of entities to load
     * @throws Exception If any exception occurred during the input file analysis.
     */
    private List<Entity> readInputFile(String filePath) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(new File(filePath), new TypeReference<List<Entity>>() {});
    }

    /**
     * Create a new OrgUnit Dspace object for an entity.
     * @param entity: the entity data to create
     * @param parent: the parent DSO object for this entity (could be null)
     * @param collection: the 'OrgUnit' holding collection
     * @param entityLevel: the depth of this entity is the entities tree.
     * @throws Exception If any exception occurred during entity creation.
     */
    private void createEntity(Entity entity, Item parent, Collection collection, int entityLevel) throws Exception {
        String padding = "  ".repeat(entityLevel);
        System.out.printf("Performing :: %s%s%n", padding, entity);

        // Create the DSO object and assign it metadata.
        PackageParameters params = new PackageParameters();
        params.setUseCollectionTemplate(true);
        params.setWorkflowEnabled(false);
        Item item = (Item)PackageUtils.createDSpaceObject(context, collection, Constants.ITEM, null, null, params);
        addEntityMetadata(item, entity, parent);
        item.setDiscoverable(entity.discoverable);
        WorkspaceItem wsi = workspaceItemService.findByItem(context, item);
        if (wsi != null)
            PackageUtils.finishCreateItem(context, wsi, null, params);
        // Update the object to make sure all changes are committed and commit item creation
        PackageUtils.updateDSpaceObject(context, item);
        this.context.commit();

        // If this entity has some children, then create an entity for each one (recursively)
        if (entity.children != null && !entity.children.isEmpty()) {
            for (Entity child : entity.children) {
                createEntity(child, item, collection, entityLevel + 1);
            }
        }
    }

    /**
     * Adds entity metadata on the newly created DSpace item.
     * @param item: The Dspace item on which add metadata
     * @param entity: The entity (from config file) to analyze
     * @param parent: (optional) the parent Dspace item (to create parent-child relation)
     * @throws SQLException if any database exception occurred during the process.
     */
    private void addEntityMetadata(Item item, Entity entity, Item parent) throws SQLException {
        MetadataField mdField = null;
        // Adds `entity.name` into `dc.title` metadata field.
        mdField = metadataFieldService.findByString(this.context, "dc.title", '.');
        itemService.addMetadata(this.context, item, mdField, null, entity.name);
        // Adds `entity.acronym` into `oairecerif.acronym` metadata field.
        if (entity.acronym != null) {
            mdField = metadataFieldService.findByString(this.context, "oairecerif.acronym", '.');
            itemService.addMetadata(this.context, item, mdField, null, entity.acronym);
        }
        // Adds `entity.type` into `dc.type` metadata field.
        mdField = metadataFieldService.findByString(this.context, "dc.type", '.');
        String mdValue = (entity.type != null) ? entity.type : this.defaultEntityType;
        itemService.addMetadata(this.context, item, mdField, null, mdValue);
        // Adds `entity.selectable` into `organization.isSelectable` metadata field.
        mdField = metadataFieldService.findByString(this.context, "organization.isSelectable", '.');
        itemService.addMetadata(this.context, item, mdField, null, String.valueOf(entity.selectable));
        // Adds `entity.identifiers` into `organization.identifier.xxx`
        if (entity.identifiers != null && !entity.identifiers.isEmpty()) {
            for (Identifier identifier : entity.identifiers) {
                String mdString = String.format("organization.identifier.%s", identifier.type);
                mdField = metadataFieldService.findByString(this.context, mdString, '.');
                itemService.addMetadata(this.context, item, mdField, null, identifier.value);
            }
        }
        // Creates relation to parent if exists
        if (parent != null) {
            mdField = metadataFieldService.findByString(this.context, "organization.parentOrganization", '.');
            itemService.addMetadata(this.context, item, mdField, null, parent.getName(), parent.getID().toString(), 600);
        }
    }

    /**
     * Search the 'OrgUnit' collection that will hold the entities
     * @return the 'OrgUnit' parent collection
     * @throws Exception if any error occurred during the search.
     */
    private Collection getOrgUnitCollection() throws Exception {
        DiscoverQuery dq = new DiscoverQuery();
        dq.setMaxResults(1);
        dq.setQuery(String.format("search.resourcetype:Collection AND dc.title_sort:\"%s\"", this.owningCollectionName));
        DiscoverResult result = searchService.search(context, dq);
        if (result.getTotalSearchResults() == 0)
            throw new IllegalStateException("No parent collection found");
        else if (result.getTotalSearchResults() > 1)
            throw new NotUniqueResultException(String.format(
                    "Multiple '%s' collection found :: %d",
                    this.owningCollectionName,
                    result.getTotalSearchResults()
            ));
        return (Collection)result.getIndexableObjects().get(0).getIndexedObject();
    }

}

class Entity {
    public String name;
    public String acronym;
    public List<Identifier> identifiers;
    public boolean discoverable = true;
    public boolean selectable = true;
    public List<Entity> children;
    public String type;

    public String toString() {
        return (this.acronym != null)
            ? String.format("[Entity::%s - %s]", this.acronym, this.name)
            : String.format("[Entity::%s]", this.name);
    }
}

class Identifier {
    public String type;
    public String value;
}