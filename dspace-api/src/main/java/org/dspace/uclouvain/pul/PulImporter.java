/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.mediafilter.JPEGFilter;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.XSLTIngestionCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.PackageUtils;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.core.utils.CleanIdentifierFields;
import org.dspace.uclouvain.core.utils.IdentifierNormalizer;
import org.dspace.uclouvain.pul.Outcome.Decision;
import org.dspace.uclouvain.services.PublicationService;
import org.jdom2.Element;

/**
 * The import of one PUL ONIX file, step by step: read it and decide ({@link #decide}), then create the publication
 * ({@link #create}) or complete the existing one ({@link #update}); both keep the ONIX file and the cover image.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class PulImporter {

    /** Short description of the bitstream format given to the stored ONIX file. */
    static final String ONIX_FORMAT = "XML";
    /** Bundle DSpace reads item thumbnails from. */
    static final String THUMBNAIL_BUNDLE = "THUMBNAIL";
    /** Marks the cover bitstreams written by this import, so that they can be refreshed. */
    static final String COVER_DESCRIPTION = "PUL cover";
    private static final Logger log = LogManager.getLogger(PulImporter.class);

    private final OnixRecordReader reader;
    private final PublicationService publicationService;
    private final UpdatePolicy updatePolicy;
    private final CoverFetcher coverFetcher;
    private final CleanIdentifierFields cleanIdentifierFields = new CleanIdentifierFields(
        DSpaceServicesFactory.getInstance().getConfigurationService());
    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private final WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance()
        .getWorkspaceItemService();
    private final InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
    private final BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    private final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private final BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
        .getBitstreamFormatService();
    private final AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();

    public PulImporter(OnixRecordReader reader, PublicationService publicationService, UpdatePolicy updatePolicy) {
        this(reader, publicationService, updatePolicy, new CoverFetcher());
    }

    public PulImporter(OnixRecordReader reader, PublicationService publicationService, UpdatePolicy updatePolicy,
                       CoverFetcher coverFetcher) {
        this.reader = reader;
        this.publicationService = publicationService;
        this.updatePolicy = updatePolicy;
        this.coverFetcher = coverFetcher;
    }

    // DECIDING ========================================================================================================

    /**
     * Read the file and decide what the import does with it. Nothing is written.
     * <p>
     * The GCOI, PUL's primary key, wins. Without a hit, the ISBNs are tried (the product's own and the one of its
     * other format), oldest publication first. A GCOI hit and an ISBN hit on different publications is an
     * ambiguity a human must resolve.
     *
     * @param context the DSpace context; must be allowed to see withdrawn and non-discoverable items.
     * @param file    the ONIX file.
     * @return the decision and how it was reached.
     */
    public Outcome decide(Context context, File file) {
        OnixRecord record;
        try {
            record = reader.read(file);
        } catch (Exception e) {
            return new Outcome(Decision.ERROR, file, null, null, "cannot read ONIX: " + e.getMessage());
        }
        try {
            return decide(context, record);
        } catch (Exception e) {
            return new Outcome(Decision.ERROR, file, record, null, "lookup failed: " + e.getMessage());
        }
    }

    private Outcome decide(Context context, OnixRecord record) throws Exception {
        if (record.isDeletion()) {
            return outcome(Decision.DELETED_NOTICE, record, null, "notification type 05, nothing is done");
        }
        if (record.gcoi() == null && record.isbns().isEmpty()) {
            return outcome(Decision.ERROR, record, null, "no GCOI and no ISBN in the record");
        }

        Optional<Publication> byGcoi = record.gcoi() == null
            ? Optional.empty()
            : publicationService.findFirstByIdentifier(context, Publication.IDENTIFIER_GCOI_FIELD, record.gcoi());

        List<Publication> byIsbn = new ArrayList<>();
        for (String isbn : record.isbns()) {
            byIsbn.addAll(publicationService.findByIdentifier(context, Publication.IDENTIFIER_ISBN_FIELD, isbn));
        }
        List<Item> isbnItems = byIsbn.stream().map(Publication::getItem).distinct().toList();

        if (byGcoi.isPresent()) {
            Item gcoiItem = byGcoi.get().getItem();
            List<Item> others = isbnItems.stream().filter(item -> !Objects.equals(item, gcoiItem)).toList();
            if (!others.isEmpty()) {
                return outcome(Decision.AMBIGUOUS, record, null, "GCOI %s -> item %s but ISBN %s -> item(s) %s"
                    .formatted(record.gcoi(), gcoiItem.getID(), record.isbns(), ids(others)));
            }
            return outcome(Decision.UPDATE, record, gcoiItem, "matched by GCOI " + record.gcoi());
        }
        if (isbnItems.isEmpty()) {
            return outcome(Decision.CREATE, record, null, "no publication with GCOI %s or ISBN %s"
                .formatted(record.gcoi(), record.isbns()) + unknownRolesNote(record));
        }
        String how = "matched by ISBN " + record.isbns();
        if (isbnItems.size() > 1) {
            how += ", %d candidates %s, oldest kept".formatted(isbnItems.size(), ids(isbnItems));
        }
        return outcome(Decision.UPDATE, record, isbnItems.get(0), how);
    }

    // CREATING ========================================================================================================

    /**
     * Create the publication a {@link Decision#CREATE} outcome stands for: a workspace item in the collection,
     * the DIM ingested, the abstracts freed of their HTML, the item installed in the archive, then the ONIX file
     * kept as an administrative bitstream. The caller commits.
     *
     * @param context    the DSpace context, in read-write mode, with the importing user as current user.
     * @param outcome    a {@link Decision#CREATE} outcome.
     * @param collection the collection to create the publication in.
     * @return the outcome completed with the created item.
     * @throws Exception if anything fails; the caller rolls back.
     */
    public Outcome create(Context context, Outcome outcome, Collection collection) throws Exception {
        if (outcome.decision() != Decision.CREATE) {
            throw new IllegalArgumentException("Not a CREATE outcome: " + outcome.describe());
        }
        OnixRecord record = outcome.record();
        WorkspaceItem workspaceItem = workspaceItemService.create(context, collection, false);
        Item item = workspaceItem.getItem();
        XSLTIngestionCrosswalk.ingestDIM(context, item, record.dim(), true);
        for (MetadataValue value : itemService.getMetadataByMetadataString(item, Publication.ABSTRACT_FIELD)) {
            value.setValue(stripHtml(value.getValue()));
        }
        itemService.update(context, item);
        Item installed = installItemService.installItem(context, workspaceItem);
        // after the installation, which gives every bundle the collection's default read policies
        storeOnix(context, installed, record.file());
        return outcome.applied(installed, "created " + installed.getHandle() + "; " + storeCover(context, installed,
            record));
    }

    // UPDATING ========================================================================================================

    /**
     * Complete the publication an {@link Decision#UPDATE} outcome points to, following the {@link UpdatePolicy}
     * for every field of the DIM but the authors. An ONIX contributor whose name is not yet an author of the item
     * is appended with its role, the other fields of the author group set to the CRIS placeholder; the existing
     * authors are never touched. {@code dc.contributor.etal} is removed once the ONIX lists named contributors only.
     * The ONIX file replaces the previous one of the same name in the administrative bundle. The caller commits.
     *
     * @param context the DSpace context, in read-write mode.
     * @param outcome an {@link Decision#UPDATE} outcome.
     * @return the outcome completed with what changed.
     * @throws Exception if anything fails; the caller rolls back.
     */
    public Outcome update(Context context, Outcome outcome) throws Exception {
        if (outcome.decision() != Decision.UPDATE) {
            throw new IllegalArgumentException("Not an UPDATE outcome: " + outcome.describe());
        }
        Item item = outcome.item();
        OnixRecord record = outcome.record();
        List<String> changes = new ArrayList<>();

        for (Map.Entry<String, List<Element>> field : dimFieldsByName(record.dim()).entrySet()) {
            String name = field.getKey();
            if (name.equals(Publication.AUTHOR_NAME_FIELD) || name.equals(Publication.AUTHOR_ROLE_FIELD)) {
                continue;
            }
            switch (updatePolicy.behaviourFor(name)) {
                case REPLACE -> replace(context, item, name, field.getValue(), changes);
                case MERGE -> merge(context, item, name, field.getValue(), changes);
                case ADD_IF_EMPTY -> addIfEmpty(context, item, name, field.getValue(), changes);
                default -> { }
            }
        }
        addMissingAuthors(context, item, record, changes);
        if (!changes.isEmpty()) {
            itemService.update(context, item);
        }
        storeOnix(context, item, record.file());
        return outcome.applied(item, (changes.isEmpty() ? "no metadata change" : "updated " + changes) + "; "
            + storeCover(context, item, record));
    }

    private void replace(Context context, Item item, String field, List<Element> onixValues, List<String> changes)
        throws Exception {
        List<String> wanted = onixValues.stream().map(value -> valueToWrite(field, value.getTextTrim())).toList();
        if (wanted.equals(values(item, field))) {
            return;
        }
        MetadataFieldName name = new MetadataFieldName(field);
        itemService.clearMetadata(context, item, name.schema, name.element, name.qualifier, Item.ANY);
        for (Element value : onixValues) {
            add(context, item, field, value);
        }
        changes.add(field);
    }

    private void merge(Context context, Item item, String field, List<Element> onixValues, List<String> changes)
        throws Exception {
        List<String> existing = values(item, field);
        boolean added = false;
        for (Element value : onixValues) {
            if (existing.stream().noneMatch(present -> sameValue(field, present, value.getTextTrim()))) {
                add(context, item, field, value);
                existing.add(value.getTextTrim());
                added = true;
            }
        }
        if (added) {
            changes.add(field);
        }
    }

    private void addIfEmpty(Context context, Item item, String field, List<Element> onixValues, List<String> changes)
        throws Exception {
        if (!values(item, field).isEmpty()) {
            return;
        }
        for (Element value : onixValues) {
            add(context, item, field, value);
        }
        changes.add(field);
    }

    private void addMissingAuthors(Context context, Item item, OnixRecord record, List<String> changes)
        throws Exception {
        List<String> onixNames = dimValues(record.dim(), Publication.AUTHOR_NAME_FIELD);
        List<String> onixRoles = dimValues(record.dim(), Publication.AUTHOR_ROLE_FIELD);
        if (onixNames.isEmpty()) {
            return;
        }
        Set<String> present = new LinkedHashSet<>();
        values(item, Publication.AUTHOR_NAME_FIELD).forEach(name -> present.add(comparableName(name)));
        Publication publication = PublicationFactory.build(item);
        int place = present.size();
        List<String> added = new ArrayList<>();
        for (int i = 0; i < onixNames.size(); i++) {
            String name = onixNames.get(i);
            if (!present.add(comparableName(name))) {
                continue;
            }
            String role = i < onixRoles.size() ? onixRoles.get(i) : null;
            if (PLACEHOLDER_PARENT_METADATA_VALUE.equals(role)) {
                role = null;
            }
            publicationService.setAuthor(context, publication, name, null, null, null, null, role, null, place++,
                false);
            added.add(name);
        }
        if (!added.isEmpty()) {
            changes.add(Publication.AUTHOR_NAME_FIELD + " +" + added.size());
        }
        if (!record.unnamedContributors() && !values(item, Publication.AUTHOR_ETAL_FIELD).isEmpty()) {
            MetadataFieldName etal = new MetadataFieldName(Publication.AUTHOR_ETAL_FIELD);
            itemService.clearMetadata(context, item, etal.schema, etal.element, etal.qualifier, Item.ANY);
            changes.add(Publication.AUTHOR_ETAL_FIELD + " removed");
        }
    }

    // METADATA HELPERS ================================================================================================

    private List<String> values(Item item, String field) {
        return new ArrayList<>(itemService.getMetadataByMetadataString(item, field).stream()
            .map(MetadataValue::getValue).toList());
    }

    private void add(Context context, Item item, String field, Element dimValue) throws Exception {
        MetadataFieldName name = new MetadataFieldName(field);
        itemService.addMetadata(context, item, name.schema, name.element, name.qualifier,
            dimValue.getAttributeValue("lang"), valueToWrite(field, dimValue.getTextTrim()));
    }

    /** Identifiers are compared in their normalized forms; anything else as trimmed text. */
    private boolean sameValue(String field, String present, String onix) {
        Optional<IdentifierNormalizer> normalizer = cleanIdentifierFields.normalizerFor(field);
        if (normalizer.isPresent()) {
            List<String> forms = normalizer.get().normalize(present);
            return normalizer.get().normalize(onix).stream().anyMatch(forms::contains);
        }
        return present.trim().equals(onix.trim());
    }

    private static String valueToWrite(String field, String value) {
        return Publication.ABSTRACT_FIELD.equals(field) ? stripHtml(value) : value;
    }

    private static String comparableName(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    static String stripHtml(String html) {
        if (html == null) {
            return null;
        }
        String text = html
            .replaceAll("(?i)<br\\s*/?>", "\n")
            .replaceAll("(?i)</(p|div|li|h[1-6])>", "\n")
            .replaceAll("<[^>]+>", "");
        text = StringEscapeUtils.unescapeHtml4(text);
        return text.replaceAll("[ \\t\\u00a0]+", " ").replaceAll(" *\\n *", "\n").replaceAll("\\n{2,}", "\n").trim();
    }

    /** The DIM fields grouped by {@code schema.element[.qualifier]}, in document order. */
    private static Map<String, List<Element>> dimFieldsByName(Element dim) {
        Map<String, List<Element>> fields = new LinkedHashMap<>();
        for (Element field : dim.getChildren()) {
            String qualifier = field.getAttributeValue("qualifier");
            String name = field.getAttributeValue("mdschema") + "." + field.getAttributeValue("element")
                + (qualifier == null || qualifier.isEmpty() ? "" : "." + qualifier);
            fields.computeIfAbsent(name, k -> new ArrayList<>()).add(field);
        }
        return fields;
    }

    private static List<String> dimValues(Element dim, String field) {
        return dimFieldsByName(dim).getOrDefault(field, List.of()).stream().map(Element::getTextTrim).toList();
    }

    // ONIX BITSTREAM ==================================================================================================

    /**
     * Keep the ONIX file with the item, in the METADATA bundle so it never counts as a publication file, replacing
     * a previous file of the same name, and without any resource policy so that only administrators can read it.
     */
    private void storeOnix(Context context, Item item, File onix) throws Exception {
        List<Bundle> bundles = item.getBundles(Constants.METADATA_BUNDLE_NAME);
        Bundle bundle = bundles.isEmpty()
            ? bundleService.create(context, item, Constants.METADATA_BUNDLE_NAME)
            : bundles.get(0);
        for (Bitstream previous : new ArrayList<>(bundle.getBitstreams())) {
            if (onix.getName().equals(previous.getName())) {
                bundleService.removeBitstream(context, bundle, previous);
            }
        }
        Bitstream bitstream;
        try (InputStream content = new FileInputStream(onix)) {
            bitstream = bitstreamService.create(context, bundle, content);
        }
        bitstream.setName(context, onix.getName());
        bitstream.setSource(context, onix.getName());
        BitstreamFormat format = PackageUtils.findOrCreateBitstreamFormat(context, ONIX_FORMAT, "application/xml",
            "ONIX record");
        bitstream.setFormat(context, format);
        bitstreamService.update(context, bitstream);
        authorizeService.removeAllPolicies(context, bundle);
        authorizeService.removeAllPolicies(context, bitstream);
    }

    // COVER IMAGE =====================================================================================================

    /**
     * Keep the ONIX front cover as the item thumbnail: downloaded, scaled by DSpace's own {@link JPEGFilter}, stored
     * in the THUMBNAIL bundle as {@code <GCOI>.pdf.jpg} (the name filter-media would give a thumbnail of the PDF,
     * so a later filter-media run neither duplicates nor overrides it). The source URL is kept on the bitstream: PUL
     * URLs embed a hash of the image, so an unchanged URL means an unchanged cover. A failure is reported, never
     * fatal for the record.
     *
     * @return one clause for the report.
     */
    private String storeCover(Context context, Item item, OnixRecord record) {
        if (record.coverUrl() == null) {
            return "no cover in ONIX";
        }
        try {
            List<Bundle> bundles = item.getBundles(THUMBNAIL_BUNDLE);
            List<Bitstream> previousCovers = bundles.stream()
                .flatMap(bundle -> bundle.getBitstreams().stream())
                .filter(bitstream -> COVER_DESCRIPTION.equals(bitstream.getDescription()))
                .toList();
            if (previousCovers.stream().anyMatch(cover -> record.coverUrl().equals(cover.getSource()))) {
                return "cover unchanged";
            }
            InputStream thumbnail;
            try (InputStream image = coverFetcher.open(record.coverUrl())) {
                thumbnail = new JPEGFilter().getDestinationStream(item, image, false);
            }
            // only once the image is in hand: a failed download must not leave an empty bundle behind
            Bundle bundle = bundles.isEmpty() ? bundleService.create(context, item, THUMBNAIL_BUNDLE) : bundles.get(0);
            Bitstream cover;
            try (thumbnail) {
                cover = bitstreamService.create(context, bundle, thumbnail);
            }
            for (Bitstream previous : previousCovers) {
                bundleService.removeBitstream(context, previous.getBundles().get(0), previous);
            }
            cover.setName(context, record.gcoi() + ".pdf.jpg");
            cover.setSource(context, record.coverUrl());
            cover.setDescription(context, COVER_DESCRIPTION);
            cover.setFormat(context, bitstreamFormatService.findByShortDescription(context, "JPEG"));
            bitstreamService.update(context, cover);
            return previousCovers.isEmpty() ? "cover stored" : "cover replaced";
        } catch (Exception e) {
            log.warn("Cover of {} not stored: {}", record.file().getName(), e.toString());
            return "cover not stored: " + e.getMessage();
        }
    }

    // HELPERS =========================================================================================================

    private static Outcome outcome(Decision decision, OnixRecord record, Item item, String message) {
        return new Outcome(decision, record.file(), record, item, message);
    }

    private static String unknownRolesNote(OnixRecord record) {
        long unknown = dimValues(record.dim(), Publication.AUTHOR_ROLE_FIELD).stream()
            .filter(PLACEHOLDER_PARENT_METADATA_VALUE::equals)
            .count();
        return unknown == 0 ? "" : "; %d contributor role(s) unknown, defaulted to author".formatted(unknown);
    }

    private static List<String> ids(List<Item> items) {
        return items.stream().map(item -> item.getID().toString()).toList();
    }
}
