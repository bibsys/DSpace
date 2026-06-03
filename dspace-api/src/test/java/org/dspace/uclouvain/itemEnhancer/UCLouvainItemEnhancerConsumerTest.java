/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.itemEnhancer.consumer.UCLouvainItemEnhancerConsumer;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Series of test for the authority based metadata enhancement functionality.
 * This functionality is configured in the 'uclouvain-metadata-enhancers.xml' configuration file.
 * 
 * A consumer ({@link UCLouvainItemEnhancerConsumer}) consumes events when an item is modified and
 * adds an entry to the enhancer queue if necessary.
 * 
 * A service ({@link UCLouvainItemEnhancerService}) holds the logic to execute the main operations of the feature.
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainItemEnhancerConsumerTest extends AbstractIntegrationTestWithDatabase {
    private ItemService itemService;
    private UCLouvainItemEnhancerService uclouvainItemEnhancerService;
    private MetadataFieldService metadataFieldService;

    private Collection collection;

    private static String[] originalConsumers;

    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final EventService eventService = EventServiceFactory.getInstance().getEventService();

    @BeforeClass
    public static void initConsumers() {
        originalConsumers = configurationService.getArrayProperty("event.dispatcher.default.consumers");
        Set<String> consumersSet = new HashSet<String>(Arrays.asList(originalConsumers));
        if (!consumersSet.contains("authoritymetadataenhancer")) {
            consumersSet.add("authoritymetadataenhancer");
            configurationService.setProperty("event.dispatcher.default.consumers", consumersSet.toArray());
            eventService.reloadConfiguration();
        }
    }

    /**
     * Reset the event.dispatcher.default.consumers property value.
     */
    @AfterClass
    public static void resetDefaultConsumers() {
        configurationService.setProperty("event.dispatcher.default.consumers", originalConsumers);
        eventService.reloadConfiguration();
    }

    // Code ran before test execution.
    @Before
    public void setup() {
        itemService = ContentServiceFactory.getInstance().getItemService();
        uclouvainItemEnhancerService = UCLouvainServiceFactory.getInstance().getItemEnhancerService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Global")
            .build();
        context.restoreAuthSystemState();
    }

    @After
    public void destroy() throws Exception {
        super.destroy();
        // Clean the enhancement table after each test execution.
        uclouvainItemEnhancerService.cleanForDateRange(context, new Date(0), new Date());
    }

    /**
     * Test a simple case where we test each action: create, update and delete:
     * 1. Test that the creation of an item adds a new entry to the database.
     * 2. Test that the update of an item adds a new entry to the database.
     * 3. Test that the deletion of an item adds a new entry to the database.
     * 
     * @throws Exception
     */
    @Test
    public void testEnhancementWithItemCreationUpdateAndDelete() throws Exception {
        // Test a simple case where we create an item:
        // 1. Create a publication with no metadata.
        // 2. Check that only one entry is in the enhancement table.

        //  1. Create a publication with no metadata.
        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .build();
        UUID publicationUUID = publication.getID();

        context.restoreAuthSystemState();
        context.commit();

        // There should be only one entry in the enhancement table.
        assertThat(uclouvainItemEnhancerService.countItemsToEnhance(context), equalTo(1));

        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(1));

        ItemToEnhance publicationEnhancement = itemToEnhanceEntries.get(0);

        assertThat(publicationEnhancement.getItemUUID(), equalTo(publicationUUID));
        assertThat(publicationEnhancement.getEntityType(), equalTo("Publication"));

        uclouvainItemEnhancerService.cleanForItem(context, publicationUUID);
        assertThat(uclouvainItemEnhancerService.countItemsToEnhance(context), equalTo(0));

        // Test a simple case where we update an item:
        // 1. Update a publication item by adding a metadata.
        // 2. We expect the table to be filled with one entry.

        publication = context.reloadEntity(publication);

        // Update the publication by adding a new metadata.
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(
            context, publication, "dc", "title", null, null, "This is a test publication");
        itemService.update(context, publication);
        context.restoreAuthSystemState();

        // Commit changes made to the item and propagate events.
        context.commit();

        // There should be still one entry in the enhancement table for the modified item.
        assertThat(uclouvainItemEnhancerService.countItemsToEnhance(context), equalTo(1));

        itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(1));

        publicationEnhancement = itemToEnhanceEntries.get(0);
        assertThat(publicationEnhancement.getItemUUID(), equalTo(publicationUUID));
        assertThat(publicationEnhancement.getEntityType(), equalTo("Publication"));

        uclouvainItemEnhancerService.cleanForItem(context, publicationUUID);
        assertThat(uclouvainItemEnhancerService.countItemsToEnhance(context), equalTo(0));

        // Test a simple case where we delete an item:
        // 1. We delete the created publication.
        // 2. We check if a new entry has been added to the database.

        publication = context.reloadEntity(publication);

        // 1. Delete the publication.
        context.turnOffAuthorisationSystem();
        itemService.delete(context, publication);
        context.restoreAuthSystemState();

        // Commit changes made to the item and propagate events.
        context.commit();

        // 2. There should be one entry in the enhancement table for the deleted item.
        assertThat(uclouvainItemEnhancerService.countItemsToEnhance(context), equalTo(1));

        itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(1));

        publicationEnhancement = itemToEnhanceEntries.get(0);
        assertThat(publicationEnhancement.getItemUUID(), equalTo(publicationUUID));
        assertThat(publicationEnhancement.getEntityType(), equalTo("Publication"));
    }

    /**
     * Test a simple case where we want to find linked items based on an authority value.
     * 
     * 1. Create a set of items: 1 profile and 2 publications linked to the profile.
     * 2. Make sure the query returns the 2 linked publications.
     * 
     * @throws Exception
     */
    @Test
    public void testGetAuthorityLinkedItem() throws Exception {
        // Create a person item and 2 corresponding publications.
        context.turnOffAuthorisationSystem();
        Item profile = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Rivpa, Jya")
            .build();
        Item publicationA = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test publication A")
            .withAuthor("Rivpa, Jya", profile.getID().toString())
            .build();

        Item publicationB = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test publication B")
            .withAuthor("Rivpa, Jya", profile.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        // Retrieve all items that mention the uuid of the person in their metadata field 'dc.contributor.author'.
        List<Pair<Item, Integer>> linkedItems = uclouvainItemEnhancerService.getAuthorityLinkedItems(
            context,
            metadataFieldService.findByElement(context, "dc", "contributor", "author"),
            profile.getID()
        );

        // Check that the 2 publications are present in the retrieved items.
        assertThat(linkedItems.size(), equalTo(2));
        assertTrue(
            linkedItems.stream()
                .map(linkedItem -> linkedItem.getLeft().getID())
                .toList()
                .containsAll(Arrays.asList(publicationA.getID(), publicationB.getID())
        ));
    }
}
