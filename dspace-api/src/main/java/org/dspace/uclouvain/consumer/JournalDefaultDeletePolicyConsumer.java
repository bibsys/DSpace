/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.util.Assert;

/**
 * Consumer to add default policies to any created Journal item.
 * This policies allows the journal managers to delete any Journal item.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class JournalDefaultDeletePolicyConsumer implements Consumer {

    private static final String JOURNAL_ENTITY_TYPE = "Journal";

    // List of action for policies to add to the item when installed.
    private static final List<Integer> actionsToAdd = Arrays.asList(Constants.DELETE, Constants.REMOVE);

    private ItemService itemService;
    private ResourcePolicyService resourcePolicyService;
    private GroupService groupService;

    private String jmgName;
    private Set<UUID> journalsToProcess = new HashSet<>();
    private static final Logger logger = LogManager.getLogger(JournalDefaultDeletePolicyConsumer.class);

    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
        resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
        groupService = EPersonServiceFactory.getInstance().getGroupService();
        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        jmgName = configService.getProperty("event.consumer.journaldefaultdeletepolicy.group");
        Assert.notNull(jmgName, "Missing setting 'event.consumer.journaldefaultdeletepolicy.group'");
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        Item item = (Item) event.getSubject(context);
        // If the created item is a Journal, add its id to the Id to process set.
        if (JOURNAL_ENTITY_TYPE.equalsIgnoreCase(itemService.getEntityType(item))) {
            journalsToProcess.add(item.getID());
        }
    }

    public void end(Context context) throws Exception {
        if (journalsToProcess.isEmpty()) {
            // Ensure that we do not continue if there are no IDs to process.
            return;
        }
        Group journalManagerGroup = groupService.findByName(context, jmgName);
        Assert.notNull(journalManagerGroup, "Unable to load group " + jmgName);
        for (UUID journalID: journalsToProcess) {
            Item journal = itemService.find(context, journalID);
            for (Integer action: actionsToAdd) {
                try {
                    ResourcePolicy deletePolicy = resourcePolicyService.create(context, null, journalManagerGroup);
                    deletePolicy.setdSpaceObject(journal);
                    deletePolicy.setAction(action);
                    resourcePolicyService.update(context, deletePolicy);
                } catch (Exception e) {
                    logger.error(
                        "Could not set default resource policy with id " + action
                        + " on Journal Item " + journal.getID(),
                        e
                    );
                    continue;
                }
            }
        }
        journalsToProcess.clear();
    }

    public void finish(Context ctx) throws Exception {}
}
