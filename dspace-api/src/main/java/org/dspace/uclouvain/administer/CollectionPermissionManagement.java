/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.administer;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Option;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.core.Constants;
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

/**
 * A command-line tool to manage permissions about a DSpace collection.
 * It allows:
 *  - Adding/removing permission for a roleType for an ePerson group.
 *  - Adding/removing a specific permission for an ePerson group.
 * To give an ePerson some management option on this group, this ePerson must be a member of the previous-named group.
 *
 * USAGE:
 *   dspace dsrun org.dspace.uclouvain.administer.CollectionPermissionManagement -c [collection_name] \
 *     -p [permission] -g [group_name] -m [mode] [--enable|--disable]
 *
 * ARGUMENTS:
 *   -c --collection:  the collection name
 *   -p, --permission: the role type (workflow group name)
 *   -g, --group:      the group name to enable/disable.
 *   -m, --mode        the mode to use for permission management ('permission' or 'role')
 *   --enable:         enable the collection management permission
 *   --disable:        disable the collection management permission
 *   -h, --help:       display collection-permission-management options.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @co-authored Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @version $Revision$
 */
public class CollectionPermissionManagement extends AbstractCLICommand {


    // CLASS CONSTANTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    public static final String USAGE_DESCRIPTION = "A command-line tool for managing collection permissions";
    public static final String ACTION_ENABLE = "enable";
    public static final String ACTION_DISABLE = "disable";
    // Available modes
    public static final String ROLE_MODE = "role";
    public static final String PERMISSION_MODE = "permission";
    // Available permissions for this CLI
    public static final Map<String, Integer> PERMISSIONS_MAP = Map.of(
        "read", Constants.READ,
        "write", Constants.WRITE,
        "delete", Constants.DELETE,
        "remove", Constants.REMOVE,
        "add", Constants.ADD,
        "admin", Constants.ADMIN,
        "withdraw_read", Constants.WITHDRAWN_READ,
        "default_bitstream_read", Constants.DEFAULT_BITSTREAM_READ,
        "default_item_read", Constants.DEFAULT_ITEM_READ
    );

    /** CLI available options */
    private static final Option OPT_COLLECTION_NAME = Option.builder("c")
            .longOpt("collection")
            .hasArg(true)
            .desc("name of the collection to manage")
            .required(true)
            .build();
    private static final Option OPT_ROLE_TYPE = Option.builder("p")
            .longOpt("permission")
            .hasArg(true)
            .desc("role type to manage or the permission to add")
            .required(true)
            .build();
    private static final Option OPT_GROUP_NAME = Option.builder("g")
            .longOpt("group")
            .hasArg(true)
            .desc("group name to enable/disable")
            .required(true)
            .build();
    private static final Option OPT_MODE = Option.builder("m")
            .longOpt("mode")
            .hasArg(true)
            .desc("The mode of permission to use ('permission' or 'role')")
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

    // CLASS ATTRIBUTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected GroupService groupService;
    protected CollectionService collectionService;
    protected WorkflowService workflowService;
    protected SearchService searchService;
    protected ResourcePolicyService resourcePolicyService;

    // CONSTRUCTOR & MAIN ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    protected CollectionPermissionManagement() {
        super();
        groupService = EPersonServiceFactory.getInstance().getGroupService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        workflowService = WorkflowServiceFactory.getInstance().getWorkflowService();
        searchService = new DSpace().getSingletonService(SearchService.class);
        resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    }

    /**
     * For invoking via the given command line arguments.
     *
     * @param argv the command line arguments given
     * @throws MissingArgumentException If a required argument is missing.
     */
    public static void main(String[] argv) throws Exception {
        CollectionPermissionManagement cpm = new CollectionPermissionManagement();
        CommandLine cl = cpm.validateCLIArgument(argv);
        String action = (cl.hasOption("disable"))
                ? CollectionPermissionManagement.ACTION_DISABLE
                : CollectionPermissionManagement.ACTION_ENABLE;
        cpm.managePermissions(
            cl.getOptionValue("c"), cl.getOptionValue("p"), cl.getOptionValue("g"), cl.getOptionValue("m"), action
        );
    }

    protected void buildOptions() {
        serviceOptions.addOption(OPT_COLLECTION_NAME);
        serviceOptions.addOption(OPT_ROLE_TYPE);
        serviceOptions.addOption(OPT_GROUP_NAME);
        serviceOptions.addOption(OPT_MODE);
        serviceOptions.addOption(OPT_ACTION_ENABLE);
        serviceOptions.addOption(OPT_ACTION_DISABLE);
        infoOptions.addOption(OPT_HELP);
    }

    protected String getUsageDescription() {
        return USAGE_DESCRIPTION;
    }


    // PRIVATE METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void managePermissions(
        String collectionName, String permission, String groupName, String mode, String action
    ) throws Exception {
        context.turnOffAuthorisationSystem();
        Collection c = findCollectionByName(collectionName);
        Group memberGroup = findGroupByName(groupName);

        switch (mode) {
            case ROLE_MODE:
                // ROLE MODE: Find the workflow group and add the member group to it.
                handleWorkflowRole(c, permission, memberGroup, action);
                break;
            case PERMISSION_MODE:
                // PERMISSION MODE: Create a resource policy and add it to the collection.
                handlePermission(c, permission, memberGroup, action);
                break;
            default:
                throw new IllegalArgumentException(
                    String.format("Unsupported mode \"%s\"", mode)
                );
        }
        collectionService.update(context, c);
        context.complete();
    }

    /**
    * Handle workflow role management for a specific collection.
    * 
    * @param c The collection to modify the workflow management for.
    * @param workflowGroupName The workflow group name to modify.
    * @param memberGroup The member group to add/remove from the workflow group.
    * @param action The action to do => add or remove (enable or disable).
    * @throws Exception
    */
    private void handleWorkflowRole(Collection c, String workflowGroupName, Group memberGroup, String action)
            throws Exception {
        Group workflowGroup = findWorkflowGroup(c, workflowGroupName);
        if (action.equals(ACTION_ENABLE)) {
            groupService.addMember(context, workflowGroup, memberGroup);
        } else {
            groupService.removeMember(context, workflowGroup, memberGroup);
        }
        groupService.update(context, workflowGroup);
    }

    /**
    * Handle a permission addition/removal for a specific group on a given collection.
    * 
    * @param c The collection to manage permission for.
    * @param permission The permission to add/remove to/from the collection.
    * @param memberGroup the group to add/remove a permission for.
    * @param action The action to do => add or remove (enable or disable permission).
    * @throws Exception
    */
    private void handlePermission(Collection c, String permission, Group memberGroup, String action) throws Exception {
        if (!PERMISSIONS_MAP.containsKey(permission)) {
            throw new IllegalArgumentException(
                String.format("Unsupported permission \"%s\"", permission)
            );
        }
        if (action.equals(ACTION_ENABLE)) {
            ResourcePolicy policy = resourcePolicyService.create(context, null, memberGroup);
            policy.setdSpaceObject(c);
            policy.setAction(PERMISSIONS_MAP.get(permission));
            resourcePolicyService.update(context, policy);
        } else {
            List<ResourcePolicy> policies =
                resourcePolicyService.find(context, c, memberGroup, PERMISSIONS_MAP.get(permission));
            if (policies.size() == 0) {
                System.out.println(
                    "No policies found to delete for the given group and action on the specified collection"
                );
                return;
            }
            policies.forEach((ResourcePolicy rp) -> {
                try {
                    resourcePolicyService.delete(context, rp);
                } catch (Exception e) {
                    System.out.println("Failed to delete permission policy with id " + rp.getID() + "\n");
                    System.out.println(e);
                }
            });
        }
    }

    /**
     * Find a ``org.dspace.content.Collection`` object by its name
     *
     * @param collectionName The collection name
     * @return The corresponding collection
     * @throws IllegalStateException if none or multiple collections are found.
     * @throws NotUniqueResultException if multiple collections are found for this name.
     * @throws SearchServiceException if exception occurred during search operations.
     */
    private Collection findCollectionByName(String collectionName) throws Exception {
        DiscoverQuery dq = new DiscoverQuery();
        dq.setMaxResults(20);
        dq.setQuery(String.format("search.resourcetype:Collection AND dc.title_sort:\"%s\"", collectionName));

        DiscoverResult result = searchService.search(context, dq);
        if (result.getTotalSearchResults() == 0) {
            throw new IllegalStateException(String.format("No collection found for %s", collectionName));
        } else if (result.getTotalSearchResults() > 1) {
            throw new NotUniqueResultException(String.format("%d collection found", result.getTotalSearchResults()));
        }
        return (Collection) result.getIndexableObjects().get(0).getIndexedObject();
    }

    /**
     * Find the collection workflow group based on its name. If the group doesn't yet exist, it will be created
     *
     * @param collection the collection to analyze
     * @param workflowRole the workflow role name to find (submitter, reviewer, editor or finaleditor)
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
            default:
                throw new IllegalArgumentException(
                        String.format("Unable to manage \"%s\" group permissions", workflowRole));
        }
    }

    /**
     * Get a user group by its name
     *
     * @param groupName the group name to search
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
