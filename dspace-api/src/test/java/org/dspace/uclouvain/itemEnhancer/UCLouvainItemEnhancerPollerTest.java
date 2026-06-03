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

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;
import org.dspace.uclouvain.itemEnhancer.poller.UCLouvainItemEnhancerPoller;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Series of test for the authority based metadata enhancement functionality.
 * This functionality is configured in the 'uclouvain-metadata-enhancers.xml' configuration file.
 * 
 * A poller {@link UCLouvainItemEnhancerPoller} will read the enhancer queue to enhance the items based on the registered actions.
 * 
 * A service ({@link UCLouvainItemEnhancerService}) holds the logic to execute the main operations of the feature.
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainItemEnhancerPollerTest extends AbstractIntegrationTestWithDatabase {
    private UCLouvainItemEnhancerService uclouvainItemEnhancerService;
    private UCLouvainItemEnhancerPoller uclouvainItemEnhancerUpdatePoller;

    private Collection collection;

    private static String[] consumers;

    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final EventService eventService = EventServiceFactory.getInstance().getEventService();

    @BeforeClass
    public static void initConsumers() {
        consumers = configurationService.getArrayProperty("event.dispatcher.default.consumers");
        Set<String> consumersSet = new HashSet<String>(Arrays.asList(consumers));
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
        configurationService.setProperty("event.dispatcher.default.consumers", consumers);
        eventService.reloadConfiguration();
    }

    // Code ran before test execution.
    @Before
    public void setup() {
        uclouvainItemEnhancerService = UCLouvainServiceFactory.getInstance().getItemEnhancerService();
        uclouvainItemEnhancerUpdatePoller = UCLouvainServiceFactory.getInstance().getItemEnhancerUpdatePoller();

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
     * Simple test to check that when an item is created, it is added to the queue and then processed by the poller.
     * The poller deletes the entry from the queue once processed.
     * 
     * @throws Exception
     */
    @Test
    public void testItemEnhancerPoller() throws Exception {
        context.turnOffAuthorisationSystem();
        ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test publication for poller")
            .build();
        context.restoreAuthSystemState();
        context.commit();

        // Check that we have 1 item waiting for enhancement in the queue (the publication).
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(1));

        // Run the enhancement.
        uclouvainItemEnhancerUpdatePoller.run();

        // Check that the queue has been cleared.
        itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(0));
    }
}
