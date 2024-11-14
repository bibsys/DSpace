/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.itemEnhancer.consumer.UCLouvainItemEnhancerConsumer;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Series of test for the authority based metadata enhancement functionality.
 * This functionality is configured in the 'uclouvain-metadata-enhancers.xml' configuration file.
 * 
 * It is composed of a consumer ({@link UCLouvainItemEnhancerConsumer}) that consume events when an item is modified and
 * adds an entry a enhancer queue if necessary.
 * 
 * A service ({@link UCLouvainItemEnhancerService}) holds the logic to execute the main operations of the feature.
 * 
 * The DAO ({@link UCLouvainItemEnhancerDAO}) interacts with the database to create, update, read and delete entries.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainItemEnhancerTest extends AbstractIntegrationTestWithDatabase {
    private ItemService itemService;

    private UCLouvainItemEnhancerService uclouvainItemEnhancerService;

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

    // Code ran before test execution.
    @Before
    public void setup() {
        itemService = ContentServiceFactory.getInstance().getItemService();
        uclouvainItemEnhancerService = UCLouvainServiceFactory.getInstance().getItemEnhancerService();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Global")
            .build();
        context.restoreAuthSystemState();
    }

    /**
     * Test a simple case where we have no metadata enhancement at all.
     * We create a publication and a person which are not linked by any metadata authority at all.
     * 
     * If we update the name of the person it should not add any entry to the database.
     * 
     * @throws Exception
     */
    @Test
    public void testConsumerWithNoEnhancement() throws Exception {
        //  1. Create a publication and a person with no link.
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Pierre Kiroul")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication0")
            .withEntityType("Publication")
            .build();

        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);

        // 2. Update the source metadata.
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(context, person, "dc", "title", null, null, "Jya Rivpa");
        itemService.update(context, person);

        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);
        publication = context.reloadEntity(publication);

        // The name of the person should have changed.
        assertThat(person.getMetadata(), hasItem(with("dc.title", "Jya Rivpa")));

        // There should be no entry at all in the enhancement table.
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.retrieveAllItemsToUpdate(context);
        assertThat(itemToEnhanceEntries, hasSize(0));
    }

    /**
     * Create a simple relation between a person and a publication where the person is an author of the publication.
     * We modify a field that is not configured as a source field ('person.identifier.orcid').
     * After we modified the field nothing should be added to the table since the modified source
     * field is not in the config.
     * 
     * @throws Exception
     */
    @Test
    public void testConsumerWithUntrackedField() throws Exception {
        // Create a basic relation with a person and a publication.
        // The person is the author of the publication.
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Pierre Kiroul")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication0")
            .withEntityType("Publication")
            .withAuthor("Pierre Kiroul", person.getID().toString())
            .build();

        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);
        publication = context.reloadEntity(publication);

        // Update an untracked field for the person item.
        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, person, "person", "identifier", "orcid", null, "0000-1111-2222-3333");
        itemService.update(context, person);
        context.restoreAuthSystemState();

        // Commit and save the changes.
        context.commit();
        context.reloadEntity(person);

        // Check that the database table is still empty.
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.retrieveAllItemsToUpdate(context);
        assertThat(itemToEnhanceEntries, hasSize(0));
    }

    /**
     * Create a simple author-publication relation and modify one entity type.
     * Test that if we modify the entity type of one of the items to something that is not handled by
     * the configuration, nothing is added to the database.
     * Since the consumer checks the entity type of the source and the target item before adding an
     * entry to the database, it should not add anything for enhancement.
     * 
     * @throws Exception
     */
    @Test
    public void testConsumerSimplePublicationAuthorRelationWithChangedEntityType() throws Exception {
        // Create a basic relation with a person and a publication.
        // The person is the author of the publication.
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Pierre Kiroul")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication0")
            .withEntityType("Publication")
            .withAuthor("Pierre Kiroul", person.getID().toString())
            .build();

        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);
        publication = context.reloadEntity(publication);

        // Change the entity type of the publication to 'Patent'.
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(context, publication, "dspace", "entity", "type", null, "Patent");
        itemService.update(context, publication);

        // Then change a metadata that is present in the configuration.
        itemService.setMetadataSingleValue(context, person, "dc", "title", null, null, "Jya Rivpa");
        itemService.update(context, person);
        context.restoreAuthSystemState();

        // Reload objects to be sure that they are up to date.
        context.commit();
        publication = context.reloadEntity(publication);
        person = context.reloadEntity(person);

        // Check that the database table is still empty.
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.retrieveAllItemsToUpdate(context);
        assertThat(itemToEnhanceEntries, hasSize(0));
    }

    /**
     * Test a single relation between a publication and an author.
     * When the author title is updated, a new entry should be added to the database table.
     * 
     * - First we add 2 items: A publication and a person. The person object has a given name of 'Pierre Kiroul'
     *   and the publication references this name in its field 'dc.contributor.author'.
     * - Then we change the 'dc.title' of the person to 'Jya Rivpa'. This should trigger the consumer which will
     *   create a new entry in the itemEnhancer table. We check that this entry exists and has the correct values.
     *
     * @throws Exception
     */
    @Test
    public void testConsumerWithSimplePublicationAuthorRelation() throws Exception {
        //  1. Create a publication and add authority controlled metadata to it.
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Pierre Kiroul")
            .build();
        UUID personAuthority = person.getID();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication0")
            .withEntityType("Publication")
            .withAuthor("Pierre Kiroul", personAuthority.toString())
            .build();

        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);

        // Make sure that the enhancement table is empty before updating.
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.retrieveAllItemsToUpdate(context);
        assertThat(itemToEnhanceEntries, hasSize(0));

        // 2. Update the source metadata.
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(context, person, "dc", "title", null, null, "Jya Rivpa");
        itemService.update(context, person);
        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);
        publication = context.reloadEntity(publication);

        assertThat(person.getMetadata(), hasItem(with("dc.title", "Jya Rivpa")));

        // Check the database entries to see if something was added.
        itemToEnhanceEntries = uclouvainItemEnhancerService.retrieveAllItemsToUpdate(context);

        // Check the entry of the database.
        assertThat(itemToEnhanceEntries, hasSize(1));
        assertThat(itemToEnhanceEntries.get(0), not(nullValue()));
        assertThat(itemToEnhanceEntries.get(0).getTargetItem(), equalTo(publication));
        assertThat(itemToEnhanceEntries.get(0).getSourceItem(), equalTo(person));
    }

    /**
     * Create two basic relations between a person (author) and a publication:
     * - Author affiliation (person.affiliation.name) -> Publication affiliation (oairecerif.author.affiliation)
     * - Author name (dc.title) -> Publication author (dc.contributor.author)
     * Here we update the last relation by changing the affiliation of the author.
     * It should create 1 entry in the database.
     * 
     * @throws Exception
     */
    @Test
    public void testConsumerWithSimplePublicationAuthorRelationMultipleFieldConfiguration() throws Exception {
        // Create a simple author-publication relation.
        // The publication is linked to the author via the name and the affiliation.
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Pierre Kiroul")
            .withPersonMainAffiliation("UCL")
            .build();
        UUID personAuthority = person.getID();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication0")
            .withEntityType("Publication")
            .withAuthor("Pierre Kiroul", personAuthority.toString())
            .withAuthorAffiliation("UCL", personAuthority.toString())
            .build();

        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);
        publication = context.reloadEntity(publication);

        // Modify the affiliation of the person.
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(context, person, "person", "affiliation", "name", null, "UCLouvain");
        itemService.update(context, person);
        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);
        publication = context.reloadEntity(publication);

        // Check that the table contains 1 entry for the modified relation.
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.retrieveAllItemsToUpdate(context);
        assertThat(itemToEnhanceEntries, hasSize(1));
    }

    /**
     * Test with one relation using multiple metadata field as source.
     * 
     * In this test we create a relation between a person and a publication.
     * The person is the source of data and the publication is the target (which will be updated).
     * The relation is configured to have 2 metadata source fields: 'dc.title' and 'person.givenName'.
     * 
     * We need to test that if we have the two metadata for one person, the first that is found is
     * the first that is used.
     * 
     * @throws Exception
     */
    @Test
    public void testConsumerWithSimplePublicationAuthorRelationAndMultipleSourceMetadataField() throws Exception {
        // First we create the person and the publication.
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withPersonIdentifierFirstName("Pierre")
            .build();
        UUID personAuthority = person.getID();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication0")
            .withEntityType("Publication")
            .withAuthor("Pierre", personAuthority.toString())
            .build();
        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);
        publication = context.reloadEntity(publication);

        // Modify the second valid metadata for the source item 'person'.
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(context, person, "person", "givenName", null, null, "Jean");
        itemService.update(context, person);
        context.restoreAuthSystemState();

        context.commit();
        context.reloadEntity(person);

        // Check that we have one entry in the database since we modified one of configured the valid
        // source metadata field.
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.retrieveAllItemsToUpdate(context);
        assertThat(itemToEnhanceEntries, hasSize(1));
    }

    /**
     * Test that when an entry is already present in the database, it updates the date when trying to insert.
     * 
     * - First we add 2 items: A publication and a person. The person object has a given name of 'Pierre Kiroul'
     *   and the publication references this name in its field 'dc.contributor.author'.
     * - Then we change the 'dc.title' of the person to 'Jya Rivpa'. This should trigger the consumer which will
     *   create a new entry in the itemEnhancer table. We check that this entry exists and has the correct values.
     * - Finally, we change the 'dc.title' of the person one more time to 'Jean Gloutitou'. This should once again
     *   trigger the consumer but this time it should just modify the 'queued_date' of the existing entry.
     *
     * @throws Exception
     */
    @Test
    public void testConsumerWithSimplePublicationAuthorRelationDateUpdate() throws Exception {
        //  1. Create a publication and add authority controlled metadata to it.
        context.turnOffAuthorisationSystem();
        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Pierre Kiroul")
            .build();
        UUID personAuthority = person.getID();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Test publication0")
            .withEntityType("Publication")
            .withAuthor("Pierre Kiroul", personAuthority.toString())
            .build();

        context.commit();
        publication = context.reloadEntity(publication);
        person = context.reloadEntity(person);

        // 2. Update the source metadata.
        itemService.setMetadataSingleValue(context, person, "dc", "title", null, null, "Jya Rivpa");
        itemService.update(context, person);
        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);

        assertThat(person.getMetadata(), hasItem(with("dc.title", "Jya Rivpa")));

        // Check the database entries to see if something was added.
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.retrieveAllItemsToUpdate(context);
        assertThat(itemToEnhanceEntries, hasSize(1));
        assertThat(itemToEnhanceEntries.get(0), not(nullValue()));
        assertThat(itemToEnhanceEntries.get(0).getDateQueued(), not(nullValue()));

        Date firstDate = itemToEnhanceEntries.get(0).getDateQueued();

        // Update the authors name a second time, the entry date should be changed.
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(context, person, "dc", "title", null, null, "Jean Gloutitou");
        itemService.update(context, person);
        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);
        publication = context.reloadEntity(publication);

        assertThat(person.getMetadata(), hasItem(with("dc.title", "Jean Gloutitou")));

        // Check the updated version of the entry.
        itemToEnhanceEntries = uclouvainItemEnhancerService.retrieveAllItemsToUpdate(context);
        assertThat(itemToEnhanceEntries, hasSize(1));
        assertThat(itemToEnhanceEntries.get(0), not(nullValue()));
        assertThat(itemToEnhanceEntries.get(0).getDateQueued(), not(nullValue()));

        // Check that the updated date is newer.
        assertThat(itemToEnhanceEntries.get(0).getDateQueued(), greaterThan(firstDate));
    }

    /**
     * Test the enhancement on multiple items.
     * In this scenario, we create 4 different items:
     * - 2 Authors both referenced in 2 publication.
     *
     * Both publication are linked to the persons by 2 metadata 'dc.contributor.author' and 'authors.email'.
     * When we update authors, it should create entries in the database for each Publication-Author relation.
     * So in this case 4 relations - 4 entries.
     * 
     *  person(title && affiliation) -> publication(author && affiliation)
     *  person(title && affiliation) -> publication2(author && affiliation)
     *  person2(title && affiliation) -> publication(author && affiliation)
     *  person2(title && affiliation) -> publication2(author && affiliation)
     * 
     * @throws Exception
     */
    @Test
    public void testConsumerMultipleConfigurationsAndMultipleEnhancement() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create initial items - 2 persons and 2 publications
        Item person = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Pierre Kiroul")
            .withPersonMainAffiliation("UCL")
            .build();

        Item person2 = ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Jean Gloutitou")
            .withPersonMainAffiliation("UNamur")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("First publication")
            .withAuthor("Pierre Kiroul", person.getID().toString())
            .withAuthor("Jean Gloutitou", person2.getID().toString())
            .withAuthorAffiliation("UCL", person.getID().toString())
            .withAuthorAffiliation("UNamur", person2.getID().toString())
            .withEntityType("Publication")
            .build();

        Item publication2 = ItemBuilder.createItem(context, collection)
            .withTitle("Second publication")
            .withAuthor("Pierre Kiroul", person.getID().toString())
            .withAuthor("Jean Gloutitou", person2.getID().toString())
            .withAuthorAffiliation("UCL", person.getID().toString())
            .withAuthorAffiliation("UNamur", person2.getID().toString())
            .withEntityType("Publication")
            .build();

        String personUUID = person.getID().toString();
        String person2UUID = person2.getID().toString();
        String publicationUUID = publication.getID().toString();
        String publication2UUID = publication2.getID().toString();

        context.commit();
        person = context.reloadEntity(person);
        person2 = context.reloadEntity(person2);
        publication = context.reloadEntity(publication);
        publication2 = context.reloadEntity(publication2);

        // Now we should have all our metadata linked via the authority.
        assertThat(
            publication.getMetadata(),
            hasItem(with("dc.contributor.author", "Pierre Kiroul", personUUID, 0, Choices.CF_ACCEPTED))
        );
        assertThat(
            publication.getMetadata(),
            hasItem(with("dc.contributor.author", "Jean Gloutitou", person2UUID, 1, Choices.CF_ACCEPTED))
        );
        assertThat(
            publication2.getMetadata(),
            hasItem(with("dc.contributor.author", "Pierre Kiroul", personUUID, 0, Choices.CF_ACCEPTED))
        );
        assertThat(
            publication2.getMetadata(),
            hasItem(with("dc.contributor.author", "Jean Gloutitou", person2UUID, 1, Choices.CF_ACCEPTED))
        );

        assertThat(
            publication.getMetadata(),
            hasItem(with("oairecerif.author.affiliation", "UCL", personUUID, 0, Choices.CF_ACCEPTED))
        );
        assertThat(
            publication.getMetadata(),
            hasItem(with("oairecerif.author.affiliation", "UNamur", person2UUID, 1, Choices.CF_ACCEPTED))
        );
        assertThat(
            publication2.getMetadata(),
            hasItem(with("oairecerif.author.affiliation", "UCL", personUUID, 0, Choices.CF_ACCEPTED))
        );
        assertThat(
            publication2.getMetadata(),
            hasItem(with("oairecerif.author.affiliation", "UNamur", person2UUID, 1, Choices.CF_ACCEPTED))
        );

        // Modify the title && affiliation for persons.
        context.turnOffAuthorisationSystem();
        itemService.setMetadataSingleValue(context, person, "dc", "title", null, null, "Jya Rivpa");
        itemService.setMetadataSingleValue(context, person, "person", "affiliation", "name", null, "UCLouvain");
        itemService.update(context, person);
        itemService.setMetadataSingleValue(context, person2, "dc", "title", null, null, "Jean Prendraideu");
        itemService.setMetadataSingleValue(context, person2, "person", "affiliation", "name", null, "UCLouvain");
        itemService.update(context, person2);
        context.restoreAuthSystemState();

        context.commit();
        person = context.reloadEntity(person);
        person2 = context.reloadEntity(person2);
        publication = context.reloadEntity(publication);
        publication2 = context.reloadEntity(publication2);

        // Check that the table has 4 entries (4 Person-Publication).
        List<ItemToEnhance> itemToEnhanceEntries = uclouvainItemEnhancerService.retrieveAllItemsToUpdate(context);
        assertThat(itemToEnhanceEntries, hasSize(4));
        // At least one entry should have the 'person' as source item.
        assertTrue(itemToEnhanceEntries.stream().anyMatch((ItemToEnhance ite) -> {
            return ite.getSourceItem().getID().toString().equals(personUUID);
        }));
        // At least one entry should have the 'person2' as source item.
        assertTrue(itemToEnhanceEntries.stream().anyMatch((ItemToEnhance ite) -> {
            return ite.getSourceItem().getID().toString().equals(person2UUID);
        }));
        // At least one entry should have the 'publication' as target item.
        assertTrue(itemToEnhanceEntries.stream().anyMatch((ItemToEnhance ite) -> {
            return ite.getTargetItem().getID().toString().equals(publicationUUID);
        }));
        // At least one entry should have the 'publication2' as target item.
        assertTrue(itemToEnhanceEntries.stream().anyMatch((ItemToEnhance ite) -> {
            return ite.getTargetItem().getID().toString().equals(publication2UUID);
        }));
    }
}
