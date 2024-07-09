package org.dspace.uclouvain.administer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.uclouvain.exceptions.NotUniqueResultException;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowService;
import org.dspace.workflow.factory.WorkflowServiceFactory;

import java.sql.SQLException;

/**
 * A command-line tool to manage permissions about a DSpace collection. It allows to
 * add/remove permission for a roleType for a ePerson group. To give an ePerson some
 * management option on this group, this ePerson must be member of the previous-named group.
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.CollectionPermissionManagement -c [collection_name] \
 *     -r [role_type] -g [group_name] [--enable|--disable]
 *
 * ARGUMENTS:
 *   -c --collection:  the collection name
 *   -r, --role:       the role type (workflow group name)
 *   -g, --group:      the group name to enable/disable.
 *   --enable:         enable the collection management permission
 *   --disable:        disable the collection management permission
 *   -h, --help:       display collection-permission-management options.
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 * @version $Revision$
 */
public class CollectionPermissionManagement extends AbstractCLICommand {


    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /** CLI available options */
    private static final Option OPT_HELP = Option.builder("h")
            .longOpt("help")
            .hasArg(false)
            .desc("explain collection-permission-management options")
            .build();
    private static final Option OPT_COLLECTION_NAME = Option.builder("c")
            .longOpt("collection")
            .hasArg(true)
            .desc("name of the collection to manage")
            .required(true)
            .build();
    private static final Option OPT_ROLE_TYPE = Option.builder("r")
            .longOpt("role")
            .hasArg(true)
            .desc("role type to manage (aka. workflow group)")
            .required(true)
            .build();
    private static final Option OPT_GROUP_NAME = Option.builder("g")
            .longOpt("group")
            .hasArg(true)
            .desc("group name to enable/disable")
            .required(true)
            .build();
    private static final Option OPT_ACTION_ENABLE = Option.builder(null)
            .longOpt("enable")
            .desc("enable permission for this group")
            .build();
    private static final Option OPT_ACTION_DISABLE = Option.builder(null)
            .longOpt("disable")
            .desc("force operation")
            .build();

    public static final String USAGE_DESCRIPTION = "A command-line tool for managing collection permissions";
    public static final String ACTION_ENABLE = "enable";
    public static final String ACTION_DISABLE = "disable";


    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected GroupService groupService;
    protected CollectionService collectionService;
    protected WorkflowService workflowService;
    protected SearchService searchService;


    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * For invoking via the given command line arguments.
     *
     * @param argv: the command line arguments given
     * @throws MissingArgumentException : If a required argument is missing.
     */
    public static void main(String[] argv) throws Exception {
        CollectionPermissionManagement cpm = new CollectionPermissionManagement();
        CommandLine cl = cpm.validateCLIArgument(argv);
        String action = (cl.hasOption("disable"))
                ? CollectionPermissionManagement.ACTION_DISABLE
                : CollectionPermissionManagement.ACTION_ENABLE;
        cpm.managePermissions(cl.getOptionValue("c"), cl.getOptionValue("r"), cl.getOptionValue("g"), action);
    }

    protected CollectionPermissionManagement() {
        super();
        groupService = EPersonServiceFactory.getInstance().getGroupService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
        searchService = new DSpace().getSingletonService(SearchService.class);
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_COLLECTION_NAME);
        serviceOptions.addOption(OPT_ROLE_TYPE);
        serviceOptions.addOption(OPT_GROUP_NAME);
        serviceOptions.addOption(OPT_ACTION_ENABLE);
        serviceOptions.addOption(OPT_ACTION_DISABLE);

        infoOptions.addOption(OPT_HELP);
    }
    protected String getUsageDescription() {
        return CollectionPermissionManagement.USAGE_DESCRIPTION;
    }


    // PRIVATE METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void managePermissions(String collectionName, String workflowRole, String groupName, String action)
    throws Exception {
        context.turnOffAuthorisationSystem();
        Collection c = findCollectionByName(collectionName);
        Group workflowGroup = findWorkflowGroup(c, workflowRole);
        Group memberGroup = findGroupByName(groupName);

        groupService.addMember(context, workflowGroup, memberGroup);
        groupService.update(context, workflowGroup);
        collectionService.update(context, c);
        context.complete();
    }

    /**
     * Find a ``org.dspace.content.Collection`` object by its name
     *
     * @param collectionName: The collection name
     * @return The corresponding collection
     * @throws IllegalStateException if none or multiple collections are found.
     * @throws NotUniqueResultException: if multiple collections are found for this name.
     * @throws SearchServiceException if exception occurred during search operations.
     */
    private Collection findCollectionByName(String collectionName) throws Exception {
        DiscoverQuery dq = new DiscoverQuery();
        dq.setMaxResults(20);
        dq.setQuery(String.format("search.resourcetype:Collection AND dc.title:\"%s\"", collectionName));

        DiscoverResult result = searchService.search(context, dq);
        if (result.getTotalSearchResults() == 0)
            throw new IllegalStateException(String.format("No collection found for %s", collectionName));
        else if (result.getTotalSearchResults() > 1)
            throw new NotUniqueResultException(String.format("%d collection found", result.getTotalSearchResults()));
        return (Collection)result.getIndexableObjects().get(0).getIndexedObject();
    }

    /**
     * Find the collection workflow group based on its name. If the group doesn't yet exist, it will be created
     *
     * @param collection: the collection to analyze
     * @param workflowRole: the workflow role name to find (submitter, reviewer, editor or finaleditor)
     * @return the requested workflow group
     * @throws Exception if any exception occurred during the process.
     */
    private Group findWorkflowGroup(Collection collection, String workflowRole) throws Exception {
        switch (workflowRole) {
            case "submitter":
                return collectionService.createSubmitters(context, collection);
            case "reviewer":
            case "editor":
            case "finaleditor":
                Group group = workflowService.getWorkflowRoleGroup(context, collection, workflowRole, null);
                if (group == null) {
                    group = workflowService.createWorkflowRoleGroup(context, collection, workflowRole);
                }
                return group;
        }
        throw new IllegalArgumentException(String.format("Unable to manage \"%s\" group permissions", workflowRole));
    }

    /**
     * Get a user group by its name
     *
     * @param groupName: the group name to search
     * @return the corresponding group (if exists)
     * @throws SQLException if exception occurred during requesting database
     * @throws IllegalStateException if no corresponding group could be found.
     */
    private Group findGroupByName(String groupName) throws SQLException, IllegalStateException {
        Group g = groupService.findByName(context, groupName);
        if (g == null) {
            throw new IllegalStateException(String.format("Group %s cannot be found", groupName));
        }
        return g;
    }

}
