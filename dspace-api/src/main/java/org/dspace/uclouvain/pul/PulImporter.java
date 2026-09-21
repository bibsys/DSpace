/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import static org.dspace.content.authority.Choices.CF_UNSET;
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
import org.dspace.submit.model.AccessConditionOption;
import org.dspace.submit.model.UploadConfigurationService;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.core.utils.CleanIdentifierFields;
import org.dspace.uclouvain.core.utils.IdentifierNormalizer;
import org.dspace.uclouvain.pul.Outcome.Decision;
import org.dspace.uclouvain.services.PublicationService;
import org.dspace.utils.DSpace;
import org.jdom2.Element;

/**
 * The import of one PUL ONIX file, step by step: read it and decide ({@link #decide}), then create the publication
 * ({@link #create}) or complete the existing one ({@link #update}); both keep the ONIX file and the cover image.
 * PDF files delivered apart are attached to their publication by {@link #decidePdf} and {@link #storePdf}.
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
    /** Item field fed by the ONIX EpubLicense; its presence makes the PDF open access. */
    static final String LICENSE_FIELD = "dcterms.license";
    /** Names of the access conditions of access-conditions.xml applied to PDFs. */
    static final String OPEN_ACCESS = "openaccess";
    static final String RESTRICTED = "restricted";
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
     * for every field of the DIM but the authors. The CRIS author group of the existing authors is completed with
     * placeholders where values are missing, then an ONIX contributor whose name is not yet an author of the item
     * is appended with its role, the other fields of the author group set to the CRIS placeholder; the existing
     * authors' names, authorities and values are never touched. {@code dc.contributor.etal} is removed once the ONIX
     * lists named contributors only. The ONIX file replaces the previous one of the same name in the administrative
     * bundle. The caller commits.
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
            if (isAuthorGroupField(name)) {
                continue;
            }
            switch (updatePolicy.behaviourFor(name)) {
                case REPLACE -> replace(context, item, name, field.getValue(), changes);
                case MERGE -> merge(context, item, name, field.getValue(), changes);
                case ADD_IF_EMPTY -> addIfEmpty(context, item, name, field.getValue(), changes);
                default -> { }
            }
        }
        completeAuthorGroups(context, item, changes);
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

    /**
     * Repair the CRIS author group of the existing authors: every place of {@code dc.contributor.author} must have a
     * value in each of the five companion fields, the placeholder when nothing better is known. Only holes are
     * filled; names, authorities and existing values are never touched, nothing is removed or reordered. Makes a
     * replay heal publications imported before the group was written in full.
     */
    private void completeAuthorGroups(Context context, Item item, List<String> changes) throws Exception {
        int authors = values(item, Publication.AUTHOR_NAME_FIELD).size();
        int filled = 0;
        for (String field : List.of(Publication.AUTHOR_EMAIL_FIELD, Publication.AUTHOR_ORCID_FIELD,
                Publication.AUTHOR_FGS_FIELD, Publication.AUTHOR_ROLE_FIELD, Publication.AUTHOR_INSTITUTION_FIELD)) {
            for (int place = 0; place < authors; place++) {
                if (itemService.getMetadata(item, field, place) == null) {
                    itemService.setMetadataInPlace(context, item, field, null, PLACEHOLDER_PARENT_METADATA_VALUE,
                        null, place, CF_UNSET);
                    filled++;
                }
            }
        }
        if (filled > 0) {
            changes.add("author group completed (%d placeholder(s))".formatted(filled));
        }
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

    /** The CRIS author group written as a block by the stylesheet and by {@code setAuthor}, never field by field. */
    private static boolean isAuthorGroupField(String field) {
        return field.equals(Publication.AUTHOR_NAME_FIELD)
            || field.equals(Publication.AUTHOR_ROLE_FIELD)
            || field.equals(Publication.AUTHOR_EMAIL_FIELD)
            || field.equals(Publication.AUTHOR_ORCID_FIELD)
            || field.equals(Publication.AUTHOR_FGS_FIELD)
            || field.equals(Publication.AUTHOR_INSTITUTION_FIELD);
    }

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

    // PDF =============================================================================================================

    /**
     * Decide what to do with a PDF delivered by PUL, named after the GCOI (14 digits) or the ISBN (10 or 13 digits,
     * hyphens allowed) of its book: attach it to the publication carrying that identifier, or wait for that
     * publication to be imported. Nothing is written.
     *
     * @param context the DSpace context; must be allowed to see withdrawn and non-discoverable items.
     * @param pdf     the PDF file.
     * @return {@link Decision#UPDATE} with the item, {@link Decision#PENDING} or {@link Decision#ERROR}.
     */
    public Outcome decidePdf(Context context, File pdf) {
        String stem = pdf.getName().replaceFirst("(?i)\\.pdf$", "");
        String digits = stem.replaceAll("[^0-9Xx]", "");
        String field;
        if (digits.matches("\\d{14}")) {
            field = Publication.IDENTIFIER_GCOI_FIELD;
        } else if (digits.matches("[0-9Xx]{10}|\\d{13}")) {
            field = Publication.IDENTIFIER_ISBN_FIELD;
        } else {
            return new Outcome(Decision.ERROR, pdf, null, null, "file name is neither <GCOI>.pdf nor <ISBN>.pdf");
        }
        String identifier = field.equals(Publication.IDENTIFIER_GCOI_FIELD) ? "GCOI " + stem : "ISBN " + stem;
        try {
            Optional<Publication> found = publicationService.findFirstByIdentifier(context, field, stem);
            if (found.isEmpty()) {
                return new Outcome(Decision.PENDING, pdf, null, null,
                    "no publication with %s yet, left for a later run".formatted(identifier));
            }
            Item item = found.get().getItem();
            boolean openAccess = !values(item, LICENSE_FIELD).isEmpty();
            return new Outcome(Decision.UPDATE, pdf, null, item, "PDF for %s, %s, stored as %s".formatted(identifier,
                openAccess ? OPEN_ACCESS + " (" + LICENSE_FIELD + " present)" : RESTRICTED, pdfName(item, pdf)));
        } catch (Exception e) {
            return new Outcome(Decision.ERROR, pdf, null, null, "lookup failed: " + e.getMessage());
        }
    }

    /**
     * The name a PUL file gets in DSpace: {@code <GCOI>.pdf} with the publication's first GCOI, whatever the
     * delivered name (PUL also names files after the ISBN); the delivered name when the publication has no GCOI.
     */
    private String pdfName(Item item, File pdf) {
        return values(item, Publication.IDENTIFIER_GCOI_FIELD).stream().findFirst()
            .map(gcoi -> gcoi + ".pdf")
            .orElse(pdf.getName());
    }

    /**
     * Attach the PDF of a {@link #decidePdf} outcome to its publication: in the ORIGINAL bundle as
     * {@code <GCOI>.pdf} (see {@link #pdfName}), replacing a file of that name without further ado, primary
     * bitstream if there is none yet, with a single read policy taken
     * from the submission access conditions: {@code openaccess} when the item carries a {@code dcterms.license},
     * {@code restricted} (UCLouvain network) otherwise. An open access PDF also gets the license URL as its own
     * license. The caller commits.
     *
     * @param context the DSpace context, in read-write mode.
     * @param outcome an {@link Decision#UPDATE} outcome of {@link #decidePdf}.
     * @return the outcome completed with what was done.
     * @throws Exception if anything fails; the caller rolls back.
     */
    public Outcome storePdf(Context context, Outcome outcome) throws Exception {
        if (outcome.decision() != Decision.UPDATE || outcome.item() == null) {
            throw new IllegalArgumentException("Not a PDF to store: " + outcome.describe());
        }
        Item item = outcome.item();
        File pdf = outcome.file();
        List<String> licenses = values(item, LICENSE_FIELD);
        boolean openAccess = !licenses.isEmpty();

        List<Bundle> bundles = item.getBundles(Constants.CONTENT_BUNDLE_NAME);
        Bundle original = bundles.isEmpty()
            ? bundleService.create(context, item, Constants.CONTENT_BUNDLE_NAME)
            : bundles.get(0);
        String name = pdfName(item, pdf);
        boolean replaced = false;
        for (Bitstream previous : new ArrayList<>(original.getBitstreams())) {
            if (name.equals(previous.getName())) {
                bundleService.removeBitstream(context, original, previous);
                replaced = true;
            }
        }
        Bitstream bitstream;
        try (InputStream content = new FileInputStream(pdf)) {
            bitstream = bitstreamService.create(context, original, content);
        }
        bitstream.setName(context, name);
        bitstream.setSource(context, pdf.getName());
        bitstream.setFormat(context, bitstreamFormatService.guessFormat(context, bitstream));
        if (openAccess && licenses.get(0).startsWith("http")) {
            bitstreamService.setMetadataSingleValue(context, bitstream,
                new MetadataFieldName(licenseField()), null, licenses.get(0));
        }
        bitstreamService.update(context, bitstream);
        if (original.getPrimaryBitstream() == null) {
            original.setPrimaryBitstreamID(bitstream);
            bundleService.update(context, original);
        }
        authorizeService.removeAllPolicies(context, bitstream);
        String access = openAccess ? OPEN_ACCESS : RESTRICTED;
        accessCondition(access).createResourcePolicy(context, bitstream, access, null, null, null);
        return outcome.applied(item, "%s %s (%s)".formatted(replaced ? "replaced" : "stored", name, access));
    }

    /** The bitstream license field of the repository (default {@code dc.rights.license}). */
    private static String licenseField() {
        return DSpaceServicesFactory.getInstance().getConfigurationService()
            .getProperty("uclouvain.global.metadata.license.field", "dc.rights.license");
    }

    /** An access condition of the submission upload step, by name, so PDFs get the same policies as a deposit. */
    private static AccessConditionOption accessCondition(String name) {
        UploadConfigurationService uploads = new DSpace().getServiceManager()
            .getServiceByName("uploadConfigurationService", UploadConfigurationService.class);
        return uploads.getMap().values().stream()
            .flatMap(configuration -> configuration.getOptions().stream())
            .filter(option -> name.equals(option.getName()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No access condition named " + name
                + " in access-conditions.xml"));
    }

    // COVER IMAGE =====================================================================================================

    /**
     * Keep the ONIX front cover as the item thumbnail: downloaded, scaled by DSpace's own {@link JPEGFilter}, stored
     * in the THUMBNAIL bundle as {@code <GCOI>.pdf.jpg}, the publication's first GCOI like the PDF itself (the name
     * filter-media would give a thumbnail of the PDF,
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
            cover.setName(context, pdfName(item, record.file()) + ".jpg");
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
