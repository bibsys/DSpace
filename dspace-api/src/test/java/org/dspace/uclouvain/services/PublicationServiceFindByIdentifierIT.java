/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * {@link PublicationService#findByIdentifier} against the embedded Solr: the identifiers go through the indexing
 * plugin (canonical forms), the searched value through the same normalizer.
 */
public class PublicationServiceFindByIdentifierIT extends AbstractIntegrationTestWithDatabase {

    private static final String ISBN = "dc.identifier.isbn";
    private static final String GCOI = "dc.identifier.gcoi";

    private final PublicationService publicationService = UCLouvainServiceFactory.getInstance().getPublicationService();
    private final IndexingService indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
        .getServiceByName(IndexingService.class.getName(), IndexingService.class);

    private Item byGcoi;
    private Item spacedIsbn;
    private Item withdrawnSameIsbn;
    private Item privateTwoIsbns;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        Collection collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications")
            .withEntityType(Publication.ENTITY_TYPE)
            .build();
        byGcoi = ItemBuilder.createItem(context, collection)
            .withTitle("Found by GCOI")
            .withMetadata("dc", "identifier", "gcoi", "29303100227990")
            .build();
        spacedIsbn = ItemBuilder.createItem(context, collection)
            .withTitle("Spaced ISBN-13")
            .withMetadata("dc", "identifier", "isbn", "978 90 272 3430 8")
            .build();
        withdrawnSameIsbn = ItemBuilder.createItem(context, collection)
            .withTitle("Withdrawn, same ISBN")
            .withMetadata("dc", "identifier", "isbn", "9789027234308")
            .withdrawn()
            .build();
        privateTwoIsbns = ItemBuilder.createItem(context, collection)
            .withTitle("Private, print and e-book ISBN")
            .withMetadata("dc", "identifier", "isbn", "2-39061-138-9")
            .withMetadata("dc", "identifier", "isbn", "978-2-39061-248-3")
            .makeUnDiscoverable()
            .build();
        for (Item item : List.of(byGcoi, spacedIsbn, withdrawnSameIsbn, privateTwoIsbns)) {
            indexingService.indexContent(context, new IndexableItem(item), true);
        }
        indexingService.commit();
        // the authorization system stays off: the import runs with administrator rights
    }

    @Test
    public void findsByGcoiWhateverTheTyping() throws Exception {
        assertEquals(List.of(byGcoi.getID()), find(GCOI, "29303100227990"));
        assertEquals(List.of(byGcoi.getID()), find(GCOI, "GCOI 2930-3100-227990"));
        assertEquals(List.of(), find(GCOI, "29303100000000"));
    }

    @Test
    public void findsIsbnAcrossEncodingsIncludingWithdrawnItems() throws Exception {
        // both items carry 9789027234308, the withdrawn one included; oldest accessioned first
        assertEquals(List.of(spacedIsbn.getID(), withdrawnSameIsbn.getID()), find(ISBN, "9789027234308"));
        assertEquals(List.of(spacedIsbn.getID(), withdrawnSameIsbn.getID()), find(ISBN, "978-90-272-3430-8"));
        // ISBN-10 of the same book
        assertEquals(List.of(spacedIsbn.getID(), withdrawnSameIsbn.getID()), find(ISBN, "90-272-3430-2"));
    }

    @Test
    public void findsNonDiscoverableItemByAnyOfItsIsbns() throws Exception {
        assertEquals(List.of(privateTwoIsbns.getID()), find(ISBN, "9782390611387")); // ISBN-13 of 2-39061-138-9
        assertEquals(List.of(privateTwoIsbns.getID()), find(ISBN, "9782390612483")); // the e-book one
    }

    @Test
    public void firstIsTheOldestAccessioned() throws Exception {
        assertEquals(spacedIsbn.getID(),
            publicationService.findFirstByIdentifier(context, ISBN, "9789027234308").orElseThrow().getID());
        assertTrue(publicationService.findFirstByIdentifier(context, ISBN, "9999999999999").isEmpty());
    }

    @Test
    public void blankOrUnusableValueFindsNothing() throws Exception {
        assertEquals(List.of(), find(ISBN, null));
        assertEquals(List.of(), find(ISBN, "   "));
        assertEquals(List.of(), find(ISBN, "n/a"));
    }

    @Test
    public void fieldWithoutNormalizerIsRejected() {
        assertThrows(IllegalArgumentException.class,
            () -> publicationService.findByIdentifier(context, "dc.title", "anything"));
    }

    private List<UUID> find(String field, String value) throws Exception {
        return publicationService.findByIdentifier(context, field, value).stream().map(Publication::getID).toList();
    }
}
