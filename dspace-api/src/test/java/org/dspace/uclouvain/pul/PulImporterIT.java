/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.pul.Outcome.Decision;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link PulImporter}: the sample ONIX files decided, created and updated against publications indexed in the
 * embedded Solr.
 */
public class PulImporterIT extends AbstractIntegrationTestWithDatabase {

    private static final Path SAMPLES = Path.of("src", "test", "data", "pul");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final IndexingService indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
        .getServiceByName(IndexingService.class.getName(), IndexingService.class);

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    private final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    /** Serves a 300x450 JPEG for any URL and records the URLs asked; can be told to fail. */
    private static class StubCoverFetcher extends CoverFetcher {
        final List<String> requested = new ArrayList<>();
        boolean failing;

        @Override
        public InputStream open(String url) throws IOException {
            requested.add(url);
            if (failing) {
                throw new IOException("no network in tests");
            }
            BufferedImage image = new BufferedImage(300, 450, BufferedImage.TYPE_INT_RGB);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", bytes);
            return new ByteArrayInputStream(bytes.toByteArray());
        }
    }

    private final StubCoverFetcher coverFetcher = new StubCoverFetcher();
    private PulImporter importer;
    private Collection collection;
    private Item matchedByGcoi;
    private Item matchedByIsbn;
    private Item gcoiOfAmbiguous;
    private Item isbnOfAmbiguous;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        importer = new PulImporter(
            new OnixRecordReader(DSpaceServicesFactory.getInstance().getConfigurationService()),
            UCLouvainServiceFactory.getInstance().getPublicationService(),
            UpdatePolicy.fromConfiguration(DSpaceServicesFactory.getInstance().getConfigurationService()),
            coverFetcher);

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).withName("Parent Community").build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Publications").withEntityType(Publication.ENTITY_TYPE).build();
        // sample 29303100021680: same GCOI
        matchedByGcoi = ItemBuilder.createItem(context, collection).withTitle("Mons")
            .withMetadata("dc", "identifier", "gcoi", "29303100021680").withdrawn().build();
        // sample 29303100123320: no GCOI anywhere, its e-book ISBN typed with hyphens
        matchedByIsbn = ItemBuilder.createItem(context, collection).withTitle("Apprentissage")
            .withMetadata("dc", "identifier", "isbn", "978-2-87558-410-6").build();
        // sample 29303100293170: GCOI on one item, ISBN on another
        gcoiOfAmbiguous = ItemBuilder.createItem(context, collection).withTitle("Crisis (GCOI)")
            .withMetadata("dc", "identifier", "gcoi", "29303100293170").build();
        isbnOfAmbiguous = ItemBuilder.createItem(context, collection).withTitle("Crisis (ISBN)")
            .withMetadata("dc", "identifier", "isbn", "9782875585264").build();
        for (Item item : List.of(matchedByGcoi, matchedByIsbn, gcoiOfAmbiguous, isbnOfAmbiguous)) {
            indexingService.indexContent(context, new IndexableItem(item), true);
        }
        indexingService.commit();
        // the authorization system stays off: the script runs as an administrator
    }

    @Test
    public void gcoiHitIsAnUpdateWhateverTheItemVisibility() {
        Outcome outcome = importer.decide(context, sample("29303100021680"));
        assertEquals(Decision.UPDATE, outcome.decision());
        assertEquals(matchedByGcoi, outcome.item());
        assertTrue(outcome.describe(), outcome.message().contains("GCOI 29303100021680"));
        assertTrue(outcome.describe(), outcome.describe().contains("[withdrawn]"));
    }

    @Test
    public void isbnHitOnTheOtherFormatIsAnUpdate() {
        Outcome outcome = importer.decide(context, sample("29303100123320"));
        assertEquals(Decision.UPDATE, outcome.decision());
        assertEquals(matchedByIsbn, outcome.item());
        assertTrue(outcome.describe(), outcome.message().startsWith("matched by ISBN"));
    }

    @Test
    public void gcoiAndIsbnOnDifferentItemsIsAmbiguous() {
        Outcome outcome = importer.decide(context, sample("29303100293170"));
        assertEquals(Decision.AMBIGUOUS, outcome.decision());
        assertNull(outcome.item());
        assertTrue(outcome.describe(), outcome.message().contains(gcoiOfAmbiguous.getID().toString()));
        assertTrue(outcome.describe(), outcome.message().contains(isbnOfAmbiguous.getID().toString()));
    }

    @Test
    public void noHitIsACreation() {
        Outcome outcome = importer.decide(context, sample("29303100808420"));
        assertEquals(Decision.CREATE, outcome.decision());
        assertNull(outcome.item());
        assertTrue(outcome.describe(), outcome.title().startsWith("La Pédagogie"));
    }

    @Test
    public void deletionNoticeIsOnlyReported() throws Exception {
        String onix = Files.readString(sample("29303100971260").toPath())
            .replace("<NotificationType>03</NotificationType>", "<NotificationType>05</NotificationType>");
        File deleted = temporaryFolder.newFile("29303100971260.xml");
        Files.writeString(deleted.toPath(), onix);

        Outcome outcome = importer.decide(context, deleted);
        assertEquals(Decision.DELETED_NOTICE, outcome.decision());
        assertNull(outcome.item());
    }

    @Test
    public void unreadableFileIsAnError() throws Exception {
        File broken = temporaryFolder.newFile("broken.xml");
        Files.writeString(broken.toPath(), "not xml at all");

        Outcome outcome = importer.decide(context, broken);
        assertEquals(Decision.ERROR, outcome.decision());
        assertTrue(outcome.describe(), outcome.message().startsWith("cannot read ONIX"));
    }

    @Test
    public void createsTheBookFromTheDimAndKeepsTheOnixAsAdminBitstream() throws Exception {
        Outcome decided = importer.decide(context, sample("29303100808420"));
        assertEquals(Decision.CREATE, decided.decision());

        Outcome created = importer.create(context, decided, collection);
        context.commit();

        Item item = created.item();
        assertNotNull(item);
        assertEquals(Decision.CREATE, created.decision());
        assertTrue(created.message(), created.message().contains("created " + item.getHandle()));
        assertTrue(item.isArchived());
        assertEquals(collection, item.getOwningCollection());
        assertEquals("La Pédagogie en questions : Guide de l'enseignant", value(item, "dc.title"));
        assertEquals("29303100808420", value(item, "dc.identifier.gcoi"));
        assertEquals(2, itemService.getMetadataByMetadataString(item, "dc.identifier.isbn").size());
        assertEquals("text::book", value(item, "dc.type.maintype"));
        assertEquals("PUL", value(item, "dcterms.source"));
        // the ONIX descriptions were HTML
        String abstractText = value(item, "dc.description.abstract");
        assertFalse(abstractText, abstractText.contains("<"));
        assertTrue(abstractText, abstractText.startsWith("Ce guide vise"));

        List<Bundle> bundles = item.getBundles(Constants.METADATA_BUNDLE_NAME);
        assertEquals(1, bundles.size());
        Bitstream onix = bundles.get(0).getBitstreams().get(0);
        assertEquals("29303100808420.xml", onix.getName());
        assertEquals(PulImporter.ONIX_FORMAT, onix.getFormat(context).getShortDescription());
        assertTrue("admin only", authorizeService.getPolicies(context, onix).isEmpty());
        assertTrue("admin only", authorizeService.getPolicies(context, bundles.get(0)).isEmpty());
        assertTrue(item.getBundles("ORIGINAL").isEmpty());
    }

    @Test
    public void aCreatedBookIsFoundByTheNextRun() throws Exception {
        Item item = importer.create(context, importer.decide(context, sample("29303100808420")), collection).item();
        context.commit();
        indexingService.indexContent(context, new IndexableItem(item), true);
        indexingService.commit();

        Outcome again = importer.decide(context, sample("29303100808420"));
        assertEquals(Decision.UPDATE, again.decision());
        assertEquals(item, again.item());
        assertTrue(again.message(), again.message().contains("GCOI 29303100808420"));
    }

    // UPDATE ==========================================================================================================

    /**
     * A hand-made notice of sample 29303100971260 (TARK proceedings), with an ISBN no fixture of {@link #setUp}
     * carries: what the update lists let the ONIX change.
     */
    private Item existingTark(Item authorProfile) {
        return ItemBuilder.createItem(context, collection)
            .withTitle("Old title")
            .withMetadata("dc", "description", "abstract", "Old abstract")
            .withMetadata("dc", "identifier", "isbn", "978-2-87463-077-4")
            .withMetadata("dc", "subject", null, "Existing subject")
            .withMetadata("dc", "date", "issued", "2015-01-01")
            .withMetadata("dc", "type", "subtype", "conference-proceedings")
            .withMetadata("dc", "contributor", "etal", "true")
            .withAuthor("Samet, Dov", authorProfile.getID().toString())
            .withMetadata("authors", "role", null, "author")
            .build();
    }

    @Test
    public void updateFollowsTheThreeListsAndLeavesTheRestAlone() throws Exception {
        Collection persons = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons").withEntityType("Person").build();
        Item profile = ItemBuilder.createItem(context, persons).withTitle("Samet, Dov").build();
        Item existing = existingTark(profile);
        indexingService.indexContent(context, new IndexableItem(existing), true);
        indexingService.commit();

        Outcome decided = importer.decide(context, sample("29303100971260"));
        assertEquals(Decision.UPDATE, decided.decision());
        assertEquals(existing, decided.item());
        Outcome updated = importer.update(context, decided);
        context.commit();

        Item item = updated.item();
        // replace
        assertTrue(value(item, "dc.title").startsWith("Theoretical Aspects of Rationality and Knowledge"));
        assertEquals(2, values(item, "dc.description.abstract").size());
        assertTrue(value(item, "dc.description.abstract").startsWith("The biannual conferences"));
        // merge: the ONIX ISBN is the existing one typed with hyphens, nothing is added twice
        assertEquals(List.of("978-2-87463-077-4"), values(item, "dc.identifier.isbn"));
        assertEquals(List.of("29303100971260"), values(item, "dc.identifier.gcoi"));
        assertEquals(List.of("Existing subject", "Psychologie et éducation"), values(item, "dc.subject"));
        assertEquals(List.of("PUL"), values(item, "dcterms.source"));
        // add-if-empty
        assertEquals("2015-01-01", value(item, "dc.date.issued"));
        assertEquals("Presses universitaires de Louvain", value(item, "publication.editor.name"));
        assertEquals("300", value(item, "publication.numberOfPages"));
        assertEquals("eng", value(item, "dc.language.iso"));
        // never touched
        assertEquals("conference-proceedings", value(item, "dc.type.subtype"));
        // authors: Samet is already there, linked to its authority, and keeps its role; the others are appended
        assertEquals(List.of("Samet, Dov", "Conducteur, Diego", "Groupe Bidon", "Inconnu, Zoé", "Texte, Anna"),
            values(item, "dc.contributor.author"));
        assertEquals(profile.getID().toString(),
            itemService.getMetadataByMetadataString(item, "dc.contributor.author").get(0).getAuthority());
        assertEquals(List.of("author", "collaborator", "scientific_director_editor",
            "#PLACEHOLDER_PARENT_METADATA_VALUE#", "author"), values(item, "authors.role"));
        assertEquals("#PLACEHOLDER_PARENT_METADATA_VALUE#", itemService.getMetadata(item, "authors.email", 2));
        // et al. is dropped, the ONIX names its contributors
        assertEquals(List.of(), values(item, "dc.contributor.etal"));
        assertTrue(updated.message(), updated.message().contains("dc.contributor.etal removed"));
        assertEquals(1, item.getBundles(Constants.METADATA_BUNDLE_NAME).get(0).getBitstreams().size());
    }

    @Test
    public void replayingTheSameFileChangesNothing() throws Exception {
        Collection persons = CollectionBuilder.createCollection(context, parentCommunity)
            .withName("Persons").withEntityType("Person").build();
        Item existing = existingTark(ItemBuilder.createItem(context, persons).withTitle("Samet, Dov").build());
        indexingService.indexContent(context, new IndexableItem(existing), true);
        indexingService.commit();
        Item item = importer.update(context, importer.decide(context, sample("29303100971260"))).item();
        context.commit();
        int metadataCount = item.getMetadata().size();

        Outcome again = importer.update(context, importer.decide(context, sample("29303100971260")));
        context.commit();

        assertEquals(item, again.item());
        assertTrue(again.message(), again.message().contains("no metadata change"));
        assertEquals(metadataCount, item.getMetadata().size());
        assertEquals(1, item.getBundles(Constants.METADATA_BUNDLE_NAME).get(0).getBitstreams().size());
    }

    @Test
    public void contributorsOfAnAuthorlessNoticeAreAllAppended() throws Exception {
        // the withdrawn fixture matched by GCOI has no author; the ONIX has four editors and a preface writer
        Outcome decided = importer.decide(context, sample("29303100021680"));
        assertEquals(matchedByGcoi, decided.item());

        Item item = importer.update(context, decided).item();
        context.commit();

        assertEquals(List.of("Dumont, Amandine", "Thiry, Amandine", "Rousseaux, Xavier", "Campion, Jonas",
            "Préfacier, Paul"), values(item, "dc.contributor.author"));
        assertEquals(List.of("scientific_director_editor", "scientific_director_editor", "scientific_director_editor",
            "scientific_director_editor", "preface_writer"), values(item, "authors.role"));
        assertEquals("#PLACEHOLDER_PARENT_METADATA_VALUE#", itemService.getMetadata(item, "authors.email", 4));
    }

    @Test
    public void etAlIsKeptWhenTheOnixItselfHasUnnamedContributors() throws Exception {
        // sample 29303100808420: one UnnamedPersons contributor, nothing named
        Item existing = ItemBuilder.createItem(context, collection).withTitle("Pédagogie")
            .withMetadata("dc", "identifier", "isbn", "9782874630972")
            .withMetadata("dc", "contributor", "etal", "true").build();
        indexingService.indexContent(context, new IndexableItem(existing), true);
        indexingService.commit();

        Item item = importer.update(context, importer.decide(context, sample("29303100808420"))).item();
        context.commit();

        assertEquals(existing, item);
        assertEquals(List.of("true"), values(item, "dc.contributor.etal"));
        assertEquals(List.of(), values(item, "dc.contributor.author"));
    }

    // COVER ===========================================================================================================

    @Test
    public void coverIsDownloadedScaledAndKeptAsThumbnail() throws Exception {
        Outcome created = importer.create(context, importer.decide(context, sample("29303100808420")), collection);
        context.commit();
        Item item = created.item();

        assertTrue(created.message(), created.message().endsWith("cover stored"));
        assertEquals(1, coverFetcher.requested.size());
        assertTrue(coverFetcher.requested.get(0), coverFetcher.requested.get(0).contains("/THUMBNAIL/"));
        List<Bundle> thumbnails = item.getBundles(PulImporter.THUMBNAIL_BUNDLE);
        assertEquals(1, thumbnails.size());
        assertEquals(1, thumbnails.get(0).getBitstreams().size());
        Bitstream cover = thumbnails.get(0).getBitstreams().get(0);
        assertEquals("29303100808420.pdf.jpg", cover.getName());
        assertEquals(PulImporter.COVER_DESCRIPTION, cover.getDescription());
        assertEquals(coverFetcher.requested.get(0), cover.getSource());
        assertEquals("JPEG", cover.getFormat(context).getShortDescription());
        BufferedImage stored = ImageIO.read(bitstreamService.retrieve(context, cover));
        assertTrue("scaled to " + stored.getWidth() + "x" + stored.getHeight(),
            stored.getWidth() <= 175 && stored.getHeight() <= 175);
        // the thumbnail DSpace shows for the item is this cover
        assertEquals(cover, itemService.getThumbnail(context, item, false).getThumb());
    }

    @Test
    public void unchangedCoverUrlIsNotDownloadedAgain() throws Exception {
        Item item = importer.create(context, importer.decide(context, sample("29303100808420")), collection).item();
        context.commit();
        indexingService.indexContent(context, new IndexableItem(item), true);
        indexingService.commit();

        Outcome again = importer.update(context, importer.decide(context, sample("29303100808420")));
        context.commit();

        assertTrue(again.message(), again.message().endsWith("cover unchanged"));
        assertEquals(1, coverFetcher.requested.size());
        assertEquals(1, item.getBundles(PulImporter.THUMBNAIL_BUNDLE).get(0).getBitstreams().size());
    }

    @Test
    public void coverDownloadFailureDoesNotFailTheRecord() throws Exception {
        coverFetcher.failing = true;
        Outcome created = importer.create(context, importer.decide(context, sample("29303100808420")), collection);
        context.commit();

        assertEquals(Decision.CREATE, created.decision());
        assertTrue(created.item().isArchived());
        assertTrue(created.message(), created.message().contains("cover not stored: no network in tests"));
        assertTrue("no empty THUMBNAIL bundle left behind",
            created.item().getBundles(PulImporter.THUMBNAIL_BUNDLE).isEmpty());
    }

    private List<String> values(Item item, String field) {
        return itemService.getMetadataByMetadataString(item, field).stream().map(MetadataValue::getValue).toList();
    }

    private String value(Item item, String field) {
        return itemService.getMetadataByMetadataString(item, field).stream().findFirst()
            .map(MetadataValue::getValue).orElse(null);
    }

    private static File sample(String gcoi) {
        return SAMPLES.resolve(gcoi + ".xml").toFile();
    }
}
