/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.content.authority.Choices.CF_UNSET;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

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
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
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
 * Test the enhancers for publication to profile relation.
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainPublicationAuthorEnhancerTest extends AbstractIntegrationTestWithDatabase {
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
     * Test the enhancement of a publication when creating a profile.
     * 
     * #1. Create a publication that reference an author by its identifiers.
     * #2. Create a profile that has the identifiers.
     * #3. Check that the publication was updated to reference the profile.
     * 
     * @throws Exception
     */
    @Test
    public void testItemEnhancerPollerCreate() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a publication containing author identifiers and a profile containing the same identifiers.
        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test publication with creation")
            .withAuthor("Gloutitout, Jean")
            .withMetadata("authors", "identifier", "fgs", "000001")
            .withMetadata("authors", "email", null, PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "identifier", "orcid", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "institution", "code", PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        Item profile = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withMetadata("dc", "title", null, "Gloutitout, Jean")
            .withMetadata("person", "email", "official", "jean.gloutitout@test.org")
            .withMetadata("person", "identifier", "fgs", "000001")
            .build();
        String profileAuthority = profile.getID().toString();

        context.restoreAuthSystemState();
        context.commit();

        // Check that we have 2 items waiting for enhancement in the queue (the publication and the profile).
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(2));

        // Run the enhancement.
        uclouvainItemEnhancerUpdatePoller.run();

        // Check that the queue has been cleared, only one item should stay for enhancement => the publication.
        // NOTE: The publication is still in enhancement because it has been updated by the run() method.
        // Because it was updated, the consumer put it back into the table.
        itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(1));

        // Profile creation enhancer validation:
        // First, reload the publication
        publication = context.reloadEntity(publication);
        // Then check the metadata values
        List<MetadataValue> mv = publication.getMetadata();
        assertThat(mv, hasItem(with("dc.contributor.author", "Gloutitout, Jean", profileAuthority, CF_ACCEPTED)));
        assertThat(mv, hasItem(with("authors.email", "jean.gloutitout@test.org", profileAuthority, CF_ACCEPTED)));
        assertThat(mv, hasItem(with("authors.identifier.fgs", "000001", profileAuthority, CF_ACCEPTED)));
    }

    /**
     * Test the enhancement of multiple publication by creating a corresponding profile.
     * 
     * #1. Create multiple publications that reference an author by its identifiers.
     * #2. Create a profile that has the same identifiers.
     * #3. Check that the publications were updated to reference the profile.
     * 
     * @throws Exception
     */
    @Test
    public void testItemEnhancerPollerMultipleCreate() throws Exception {
        context.turnOffAuthorisationSystem();
        Item publication1 = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("First test publication for item creation")
            .withAuthor("Pah, Ramass")
            .withAuthor("Gloutitout, Jean")
            .withMetadata("authors", "identifier", "fgs", "987654")
            .withMetadata("authors", "identifier", "fgs", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "email", null, "ramass.pah@test.org")
            .withMetadata("authors", "email", null, "jean.gloutitout@test.org")
            .withMetadata("authors", "identifier", "orcid", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "identifier", "orcid", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "institution", "code", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "institution", "code", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "role", null, "author")
            .withMetadata("authors", "role", null, "author")
            .build();

        Item publication2 = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second test publication for item creation")
            .withAuthor("Gloutitout, Jean")
            .withAuthor("Rivpa, Jya")
            .withMetadata("authors", "identifier", "fgs", "000001")
            .withMetadata("authors", "identifier", "fgs", "123456")
            .withMetadata("authors", "email", null, PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "email", null, "jya.rivpa@test.org")
            .withMetadata("authors", "identifier", "orcid", "4444-5555-6666")
            .withMetadata("authors", "identifier", "orcid", "1111-2222-3333")
            .withMetadata("authors", "institution", "code", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "institution", "code", "Best university")
            .withMetadata("authors", "role", null, "author")
            .withMetadata("authors", "role", null, "author")
            .build();

        Item profile = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withMetadata("dc", "title", null, "Gloutitout, Jean")
            .withMetadata("person", "email", "official", "jean.gloutitout@test.org")
            .withMetadata("person", "identifier", "fgs", "000001")
            .withMetadata("person", "affiliation", "institution", "Test institution")
            .build();
        String profileAuthority = profile.getID().toString();

        context.restoreAuthSystemState();
        context.commit();

        // Check that we have 2 items waiting for enhancement in the queue (the publication and the profile).
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(3));

        // Run the enhancement.
        uclouvainItemEnhancerUpdatePoller.run();

        // Check that the queue has been cleared, only 2 items should stay for enhancement => the publications
        itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(2));

        // Profile creation enhancer validation:
        // First, reload the publication
        publication1 = context.reloadEntity(publication1);
        publication2 = context.reloadEntity(publication2);

        // Check the metadata values of publication 1.
        List<MetadataValue> mv = publication1.getMetadata();
        assertThat(mv, hasItem(with("dc.contributor.author", "Gloutitout, Jean", profileAuthority, 1, CF_ACCEPTED)));
        assertThat(mv, hasItem(with("authors.email", "jean.gloutitout@test.org", profileAuthority, 1, CF_ACCEPTED)));
        assertThat(mv, hasItem(with("authors.identifier.fgs", "000001", profileAuthority, 1, CF_ACCEPTED)));
        assertThat(mv, hasItem(with("authors.institution.code", "Test institution", profileAuthority, 1, CF_ACCEPTED)));
        assertThat(mv, hasItem(with(
            "authors.identifier.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, null, 1, CF_UNSET)));
        assertThat(mv, hasItem(with("authors.role", "author", null, 1, CF_UNSET)));
        // Check that the second author of publication 1 has not been updated.
        assertThat(mv, hasItem(with("dc.contributor.author", "Pah, Ramass", null, 0, CF_UNSET)));
        assertThat(mv, hasItem(with("authors.email", "ramass.pah@test.org", null, 0, CF_UNSET)));
        assertThat(mv, hasItem(with("authors.identifier.fgs", "987654", null, 0, CF_UNSET)));
        assertThat(mv, hasItem(with("authors.institution.code", PLACEHOLDER_PARENT_METADATA_VALUE, null, 0, CF_UNSET)));
        assertThat(mv, hasItem(with(
            "authors.identifier.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, null, 0, CF_UNSET)));
        assertThat(mv, hasItem(with("authors.role", "author", null, 0, CF_UNSET)));

        // Check the metadata values of publication 2.
        mv = publication2.getMetadata();
        assertThat(mv, hasItem(with("dc.contributor.author", "Gloutitout, Jean", profileAuthority, 0, CF_ACCEPTED)));
        assertThat(mv, hasItem(with("authors.email", "jean.gloutitout@test.org", profileAuthority, 0, CF_ACCEPTED)));
        assertThat(mv, hasItem(with("authors.identifier.fgs", "000001", profileAuthority, 0, CF_ACCEPTED)));
        assertThat(mv, hasItem(with("authors.institution.code", "Test institution", profileAuthority, 0, CF_ACCEPTED)));
        assertThat(mv, hasItem(with("authors.identifier.orcid", "4444-5555-6666", null, 0, CF_UNSET)));
        assertThat(mv, hasItem(with("authors.role", "author", null, 0, CF_UNSET)));
        // Check that the second author of publication 2 has not been updated.
    }

    /**
     * Test the enhancement of a publication by updating an already linked profile.
     * 
     * #1. Create a profile with specific identifiers.
     * #2. Create a publication that reference the profile by its identifiers and authority.
     * #3. Update the profile with new values.
     * #4. Check that the author fields of the publication were updated to be up to date with to the profile.
     *
     * @throws Exception
     */
    @Test
    public void testItemEnhancerPollerUpdate() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a base profile and a publication linked to it.
        Item profile = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withMetadata("dc", "title", null, "Gloutitout, Jean")
            .withMetadata("person", "email", "official", "jean.gloutitout@test.org")
            .withMetadata("person", "identifier", "fgs", "000001")
            .withMetadata("person", "affiliation", "institution", "Test institution")
            .build();
        String profileAuthority = profile.getID().toString();

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test publication with update")
            .withAuthor("Gloutitous, Jean", profileAuthority, CF_ACCEPTED)
            .withMetadata("authors", "identifier", "fgs", null, "000001", profileAuthority, CF_ACCEPTED)
            .withMetadata("authors", "email", null, PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "identifier", "orcid", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "institution", "code", "Imagine University")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        // Check that we have 2 item waiting for enhancement in the queue (the publication and the profile).
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(2));

        // Run the enhancement.
        uclouvainItemEnhancerUpdatePoller.run();

        // Profile creation enhancer validation:
        // First, reload the publication
        publication = context.reloadEntity(publication);
        List<MetadataValue> mv = publication.getMetadata();
        // Check that the metadata has been updated accordingly
        assertThat(mv, hasItem(with("dc.contributor.author", "Gloutitout, Jean", profileAuthority, CF_ACCEPTED)));
        // The email should have been updated since it was linked to the authority.
        assertThat(mv, hasItem(with("authors.email", "jean.gloutitout@test.org", profileAuthority, CF_ACCEPTED)));
        // The fgs should have been updated to have an authority since it had the same value has the source.
        assertThat(mv, hasItem(with("authors.identifier.fgs", "000001", profileAuthority, CF_ACCEPTED)));
        // CHeck that the orcid has not change since the profile has no orcid.
        assertThat(mv, hasItem(with("authors.identifier.orcid", PLACEHOLDER_PARENT_METADATA_VALUE, null, CF_UNSET)));
        // The institution should not have changed since it was not linked to the authority.
        assertThat(mv, hasItem(with("authors.institution.code", "Imagine University", null, CF_UNSET)));
    }

    /**
     * Test the enhancement of multiple publications by updating an already linked profile.
     * 
     * #1. Create a profile with specific identifiers.
     * #2. Create multiple publications that reference the profile by its identifiers and authority.
     * #3. Update the profile with new values.
     * #4. Check that the author fields of the publications were updated to be up to date with to the profile.
     * 
     * @throws Exception
     */
    @Test
    public void testItemEnhancerPollerMultipleUpdate() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create a profile and 2 publications linked to it.
        Item profile = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withMetadata("dc", "title", null, "Gloutitout, Jean")
            .withMetadata("person", "email", "official", "jean.gloutitout@test.org")
            .withMetadata("person", "identifier", "fgs", "000001")
            .withMetadata("person", "affiliation", "institution", "Test institution")
            .withMetadata("person", "identifier", "orcid", "7777-8888-9999")
            .build();
        String profileAuthority = profile.getID().toString();

        Item publication1 = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("First test publication for item creation")
            .withAuthor("Pah, Ramass")
            .withAuthor("Gloutitout, Jean", profileAuthority, CF_ACCEPTED)
            .withMetadata("authors", "identifier", "fgs", "987654")
            .withMetadata("authors", "identifier", "fgs", null, "000001", profileAuthority, CF_ACCEPTED)
            .withMetadata("authors", "email", null, "ramass.pah@test.org")
            .withMetadata("authors", "email", null, null, "jean.gloutitout@test.org", profileAuthority, CF_ACCEPTED)
            .withMetadata("authors", "identifier", "orcid", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "identifier", "orcid", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "institution", "code", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "institution", "code", PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "role", null, "author")
            .withMetadata("authors", "role", null, "translator")
            .build();

        Item publication2 = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second test publication for item creation")
            .withAuthor("Gloutitout, Jean", profileAuthority, CF_ACCEPTED)
            .withAuthor("Rivpa, Jya")
            .withMetadata("authors", "identifier", "fgs", "000001")
            .withMetadata("authors", "identifier", "fgs", "123456")
            .withMetadata("authors", "email", null, PLACEHOLDER_PARENT_METADATA_VALUE)
            .withMetadata("authors", "email", null, "jya.rivpa@test.org")
            .withMetadata("authors", "identifier", "orcid", "4444-5555-6666")
            .withMetadata("authors", "identifier", "orcid", "1111-2222-3333")
            .withMetadata("authors", "institution", "code", "UCLouvain")
            .withMetadata("authors", "institution", "code", "Best university")
            .withMetadata("authors", "role", null, "author")
            .withMetadata("authors", "role", null, "author")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        // Check that we have 2 item waiting for enhancement in the queue (the publication and the profile).
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
        assertThat(itemToEnhanceEntries.size(), equalTo(3));

        // Run the enhancement.
        uclouvainItemEnhancerUpdatePoller.run();

        // Profile creation enhancer validation:
        // First, reload the publication
        publication1 = context.reloadEntity(publication1);
        publication2 = context.reloadEntity(publication2);

        List<MetadataValue> mv = publication1.getMetadata();
        // Check that the metadata has been updated accordingly for publication n°1.
        assertThat(mv, hasItem(with("dc.contributor.author", "Gloutitout, Jean", profileAuthority, 1, CF_ACCEPTED)));
        // The email should have been updated since it was linked to the authority.
        assertThat(mv, hasItem(with("authors.email", "jean.gloutitout@test.org", profileAuthority, 1, CF_ACCEPTED)));
        // The fgs should have been updated to have an authority since it had the same value has the source.
        assertThat(mv, hasItem(with("authors.identifier.fgs", "000001", profileAuthority, 1, CF_ACCEPTED)));
        // Check that the orcid has been updated to reflect the profile orcid.
        assertThat(mv, hasItem(with("authors.identifier.orcid", "7777-8888-9999", profileAuthority, 1, CF_ACCEPTED)));
        // The institution should have changed since it was only a placeholder.
        assertThat(mv, hasItem(with("authors.institution.code", "Test institution", profileAuthority, 1, CF_ACCEPTED)));
        // Translator role should be unchanged.
        assertThat(mv, hasItem(with("authors.role", "translator", null, 1, CF_UNSET)));

        mv = publication2.getMetadata();
        // Check that the metadata has been updated accordingly for publication n°2.
        assertThat(mv, hasItem(with("dc.contributor.author", "Gloutitout, Jean", profileAuthority, 0, CF_ACCEPTED)));
        // The email should have been updated since it had no value (only placeholder).
        assertThat(mv, hasItem(with("authors.email", "jean.gloutitout@test.org", profileAuthority, 0, CF_ACCEPTED)));
        // The fgs should have been updated to have an authority since it had the same value has the source.
        assertThat(mv, hasItem(with("authors.identifier.fgs", "000001", profileAuthority, 0, CF_ACCEPTED)));
        // Check that the orcid has been updated to reflect the profile orcid.
        assertThat(mv, hasItem(with("authors.identifier.orcid", "4444-5555-6666", null, 0, CF_UNSET)));
        // The institution should not have changed since it was not linked to the authority.
        assertThat(mv, hasItem(with("authors.institution.code", "UCLouvain", null, 0, CF_UNSET)));
    }

    /**
     * Test the enhancement of a publication by updating an already linked profile.
     * We expect the author field of the publication to be updated to be up to date with to the profile information.
     * 
     * NOTE: This test is not necessary for now since we do not handle deletion for enhancement. (no enhancers defined)
     * 
     * @throws Exception
     */
    // @Test
    // public void testItemEnhancerPollerWithDeletion() throws Exception {
    //     context.turnOffAuthorisationSystem();
    //     Item publication = ItemBuilder.createItem(context, collection)
    //         .withEntityType("Publication")
    //         .withTitle("Test publication 1")
    //         .withAuthor("Gloutitout, Jean")
    //         .build();

    //     context.restoreAuthSystemState();
    //     context.commit();

    //     // Clear the enhancement table to be sure that it is empty (do not take the creation into account)
    //     uclouvainItemEnhancerService.cleanForItem(context, publication.getID());
    //     context.commit();

    //     publication = context.reloadEntity(publication);

    //     context.turnOffAuthorisationSystem();
    //     itemService.delete(context, publication);
    //     context.restoreAuthSystemState();

    //     context.commit();

    //     // Check that we have 1 item waiting for enhancement in the queue.
    //     List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
    //     assertThat(itemToEnhanceEntries.size(), equalTo(1));

    //     // Run the enhancement.
    //     uclouvainItemEnhancerUpdatePoller.run();

    //     // Check that the queue has been cleared.
    //     itemToEnhanceEntries = uclouvainItemEnhancerService.getItemsToEnhance(context);
    //     assertThat(itemToEnhanceEntries.size(), equalTo(0));
    // }
}
