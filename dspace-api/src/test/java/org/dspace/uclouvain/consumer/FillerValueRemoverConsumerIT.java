/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.WorkspaceItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.core.CrisConstants;
import org.dspace.event.factory.EventServiceFactory;
import org.dspace.event.service.EventService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FillerValueRemoverConsumerIT extends AbstractIntegrationTestWithDatabase {
    private ItemService itemService;
    private InstallItemService installItemService;
    private Collection collection;
    private static String[] consumers;

    private static final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final EventService eventService = EventServiceFactory.getInstance().getEventService();

    /**
     * Load the consumer into the configuration if it is not yet present.
     */
    @BeforeClass
    public static void loadConsumers() {
        consumers = configurationService.getArrayProperty("event.dispatcher.default.consumers");
        Set<String> consumersSet = new HashSet<>(Arrays.asList(consumers));
        if (!consumersSet.contains("fillervalueremover")) {
            consumersSet.add("fillervalueremover");
            configurationService.setProperty("event.dispatcher.default.consumers", consumersSet.toArray());
        }
        // Set the values to clear to 'N/A', 'NA', 'na' ...
        configurationService.setProperty("event.consumer.fillervalueremover.fillers", "^\\s*(?i)(n\\/?a)\\s*$");
        configurationService.addPropertyValue("event.consumer.fillervalueremover.fillers", "^\\s*(?i)(no(t|n) applicable)\\s*$");

        eventService.reloadConfiguration();
    }

    /**
     * Reset the event.dispatcher.default.consumers property value.
     */
    @AfterClass
    public static void resetDefaultConsumers() {
        configurationService.setProperty("event.dispatcher.default.consumers", consumers);
        eventService.reloadConfiguration();
    }

    @Before
    public void setup() {
        itemService = ContentServiceFactory.getInstance().getItemService();
        installItemService = ContentServiceFactory.getInstance().getInstallItemService();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
            .withName("Parent Community")
            .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publication")
            .withSubmissionDefinition("publication")
            .build();
        context.restoreAuthSystemState();
    }

    /**
     * Test a the case where a simple "onebox" field value is "N/A".
     * This field should be remove from the items metadata once the item is in the archive.
     * 
     * @throws Exception
     */
    @Test
    public void testSimpleFillerField() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create the base workspace item with the required metadata.
        WorkspaceItem publication = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("This is a test publication")
            .withAbstract("N/A")
            .build();
        // Check that no modifications were made by the consumer since we are still in workspace.
        assertThat(
            publication.getItem().getMetadata(),
            hasItem(with("dc.description.abstract", "N/A"))
        );

        installItemService.installItem(context, publication);
        context.dispatchEvents();
        context.restoreAuthSystemState();

        // Once the item is in the archive, the consumer should modify the abstract.
        String publicationAbstract = itemService.getMetadataFirstValue(
            publication.getItem(), "dc", "description", "abstract", null
        );
        String publicationTitle = itemService.getMetadataFirstValue(
            publication.getItem(), "dc", "title", null, null
        );
        assertThat(publicationAbstract, nullValue());
        assertThat(publicationTitle, equalTo("This is a test publication"));
    }

    /**
     * Test a case where we set a 'N/A' value to a field in a form group.
     * The consumer should replace the field value by the default placeholder.
     * 
     * @throws Exception
     */
    @Test
    public void testSimpleFillerFieldInGroup() throws Exception {
        context.turnOffAuthorisationSystem();
        // Create the base workspace item with the required metadata.
        WorkspaceItem publication = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withTitle("This is a test publication")
            .withAuthor("Mikel Theunis")
            .withAuthorAffilitation("N/A")
            .build();
        // Check that no modifications were made by the consumer since we are still in workspace.
        assertThat(
            publication.getItem().getMetadata(),
            hasItem(with("oairecerif.author.affiliation", "N/A"))
        );

        installItemService.installItem(context, publication);
        context.dispatchEvents();
        context.restoreAuthSystemState();

        // Once the item is in the archive, the consumer should modify the affiliation and set it to placeholder.
        String publicationAuthor = itemService.getMetadataFirstValue(
            publication.getItem(), "dc", "contributor", "author", null
        );
        // The author name should not have changed.
        assertThat(publicationAuthor, equalTo("Mikel Theunis"));
        String publicationAffiliation = itemService.getMetadataFirstValue(
            publication.getItem(), "oairecerif", "author", "affiliation", null
        );
        // The affiliation should have a value set to the default placeholder.
        assertThat(publicationAffiliation, equalTo(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE));
    }

    /**
     * Mix between normal fields and in group fields.
     * We have 2 authors and 2 affiliations:
     * - 1. 'Jean Gloutitou' as author name with a 'NA' affiliation,
     * - 2. 'N/A' as author name with a 'UCLouvain' affiliation
     * 
     * The first affiliation has to be changed to a placeholder since it is in a group field.
     * The second author name has to be changed to a placeholder since it is in a a group field.
     * 
     * Also, we have a 'Not applicable' abstract and a 'NA' url.
     * Those to field should be removed by the consumer.
     * 
     * Finally, we have a dc.title that has NA and N/A in it but should not be counted as filler:
     * "Why are 'N/A' and 'Not applicable' useful in publication deposits ?"
     * This should not change even after the consumer has processed the item.
     * 
     * @throws Exception
     */
    @Test
    public void testClearBothGroupFieldAndNormalField() throws Exception {
        context.turnOffAuthorisationSystem();
        WorkspaceItem publication = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
            .withAuthor("Jean Gloutitou")
            .withAuthorAffilitation("NA")
            .withAuthor("N/A")
            .withAuthorAffilitation("UCLouvain")
            .withTitle("Why are 'N/A' and 'Not applicable' useful in publication deposits ?")
            .withAbstract("Not applicable")
            .withCustomUrl("NA")
            .build();
        // Check that no modifications have been made since the publication is still in workspace.
        assertThat(
            publication.getItem().getMetadata(),
            hasItem(with("oairecerif.author.affiliation", "NA", 0))
        );
        assertThat(
            publication.getItem().getMetadata(),
            hasItem(with("dc.contributor.author", "N/A", 1))
        );
        assertThat(
            publication.getItem().getMetadata(),
            hasItem(with("dc.description.abstract", "Not applicable"))
        );
        assertThat(
            publication.getItem().getMetadata(),
            hasItem(with("cris.customurl", "NA"))
        );
        // Trigger item deposit.
        installItemService.installItem(context, publication);
        context.dispatchEvents();
        context.restoreAuthSystemState();

        // Check that the fields containing the 'fillers' were cleared.
        List<MetadataValue> publicationAuthors = itemService.getMetadata(
            publication.getItem(), "dc", "contributor", "author", null
        );
        List<MetadataValue> publicationAuthorsAffiliations = itemService.getMetadata(
            publication.getItem(), "oairecerif", "author", "affiliation", null
        );
        // The first author name should not have changed, the second one should be a placeholder.
        assertThat(
            publicationAuthors,
            hasItem(with("dc.contributor.author", "Jean Gloutitou", 0))
        );
        assertThat(
            publicationAuthors,
            hasItem(with("dc.contributor.author", CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE, 1))
        );

        // The first affiliation should be a placeholder, the second one should not have been changed.
        assertThat(
            publicationAuthorsAffiliations,
            hasItem(with("oairecerif.author.affiliation", CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE, 0))
        );
        assertThat(
            publicationAuthorsAffiliations,
            hasItem(with("oairecerif.author.affiliation", "UCLouvain", 1))
        );

        // The abstract 'filler' should have been removed.
        String publicationAbstract = itemService.getMetadataFirstValue(
            publication.getItem(), "dc", "description", "abstract", null
        );
        assertThat(publicationAbstract, nullValue());

        // The custom url 'filler' should have been removed.
        String publicationCustomUrl = itemService.getMetadataFirstValue(
            publication.getItem(), "dc", "description", "abstract", null
        );
        assertThat(publicationCustomUrl, nullValue());

        // Check that the title has not been change even if it contains 'N/A' and 'NA'
        String publicationTitle = itemService.getMetadataFirstValue(
            publication.getItem(), "dc", "title", null, null
        );
        assertThat(publicationTitle, equalTo("Why are 'N/A' and 'Not applicable' useful in publication deposits ?"));
    }
}
