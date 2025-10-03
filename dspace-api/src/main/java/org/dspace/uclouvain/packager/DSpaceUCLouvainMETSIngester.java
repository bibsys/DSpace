/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.packager;

import static org.dspace.content.crosswalk.XSLTCrosswalk.DIM_NS;
import static org.dspace.uclouvain.content.utils.CommentUtils.loadLegacyComments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.authority.Choices;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.packager.AbstractMETSIngester;
import org.dspace.content.packager.DSpaceMETSIngester;
import org.dspace.content.packager.METSManifest;
import org.dspace.content.packager.PackageParameters;
import org.dspace.content.packager.PackageValidationException;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.content.LegacyComment;
import org.dspace.uclouvain.content.service.CommentService;
import org.dspace.uclouvain.core.model.Journal;
import org.dspace.uclouvain.core.model.OrgUnit;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.JournalService;
import org.dspace.uclouvain.services.OrgUnitService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.WorkflowException;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;
import org.jdom2.xpath.XPathFactory;

public class DSpaceUCLouvainMETSIngester extends DSpaceMETSIngester {

    private final Namespace modsNS = Namespace.getNamespace("mods", "http://www.loc.gov/mods/v3");

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AbstractMETSIngester.class);
    private static final String bitstreamExtractorStylesheetConfigKey =
            "uclouvain.ingester.bitstreamMetadataExtractor.stylesheet";
    private static final MetadataFieldService metadataFieldService =
            ContentServiceFactory.getInstance().getMetadataFieldService();
    private static final BitstreamService bitstreamService =
            ContentServiceFactory.getInstance().getBitstreamService();
    private final CommentService commentService = UCLouvainServiceFactory.getInstance().getCommentService();
    private final ResearcherProfileService researcherProfileService =
            new DSpace().getSingletonService(ResearcherProfileService.class);
    private final OrgUnitService orgUnitService = UCLouvainServiceFactory.getInstance().getOrgUnitService();
    private final JournalService journalService = UCLouvainServiceFactory.getInstance().getJournalService();

    private long transformerLastModified = 0;
    private File transformFile;
    private Transformer transformer;

    // CONSTRUCTOR ============================================================
    public DSpaceUCLouvainMETSIngester() {
        // try to load the required configuration to extract bitstream metadata.
        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        String filename = configurationService.getProperty(bitstreamExtractorStylesheetConfigKey);
        if (filename == null) {
            log.warn("Unable to load stylesheet to extract bitstream metadata from '" +
                    bitstreamExtractorStylesheetConfigKey + "'");
            return;
        }
        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        this.transformFile = new File(parent, filename);
    }

    // OVERRIDE METHODS =======================================================
    /**
     * Ingest/import a single DSpace Object, based on the associated METS
     * Manifest and the parameters passed to the METSIngester
     *
     * @param context  DSpace Context
     * @param parent   Parent DSpace Object
     * @param manifest the parsed METS Manifest
     * @param pkgFile  the full package file (which may include content files if a
     *                 zip)
     * @param params   Parameters passed to METSIngester
     * @param license  DSpace license agreement
     * @return completed result as a DSpace object
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     * @throws CrosswalkException          if crosswalk error
     * @throws MetadataValidationException if metadata validation error
     * @throws WorkflowException           if workflow error
     * @throws PackageValidationException  if package validation error
     */
    protected DSpaceObject ingestObject(
            Context context, DSpaceObject parent, METSManifest manifest,
            File pkgFile, PackageParameters params, String license
    ) throws IOException, SQLException, AuthorizeException, CrosswalkException,
            PackageValidationException, WorkflowException {
        DSpaceObject dso = super.ingestObject(context, parent, manifest, pkgFile, params, license);
        this.addAncestorIdentifier(context, dso, manifest, params);
        this.updateObjectStatus(context, dso, manifest, params);
        return dso;
    }

    /**
     * Replace the contents of a single DSpace Object, based on the associated
     * METS Manifest and the parameters passed to the METSIngester.
     *
     * @param context  DSpace Context
     * @param dso      DSpace Object to replace
     * @param manifest the parsed METS Manifest
     * @param pkgFile  the full package file (which may include content files if a
     *                 zip)
     * @param params   Parameters passed to METSIngester
     * @param license  DSpace license agreement
     * @return completed result as a DSpace object
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     * @throws CrosswalkException          if crosswalk error
     * @throws MetadataValidationException if metadata validation error
     * @throws PackageValidationException  if package validation error
     */
    protected DSpaceObject replaceObject(
            Context context, DSpaceObject dso, METSManifest manifest,
            File pkgFile, PackageParameters params, String license
    ) throws IOException, SQLException, AuthorizeException, CrosswalkException,
            MetadataValidationException, PackageValidationException {
        dso = super.replaceObject(context, dso, manifest, pkgFile, params, license);
        this.addAncestorIdentifier(context, dso, manifest, params);
        if (params.restoreModeEnabled() && dso.getType() == Constants.ITEM && !params.workflowEnabled()) {
            this.populateMetadata(context, (Item)dso);
        }
        this.updateObjectStatus(context, dso, manifest, params);
        return dso;
    }

    /**
     * Enable `Item` withdrawn status if the METS manifest record status has 'inactive' value
     *
     * @param context     DSpace context
     * @param dso         DSpace object to manage
     * @param manifest    The parse METS manifest
     * @param params      Parameters passed to METSIngester
     *
     * @throws SQLException        if database error
     * @throws AuthorizeException  if authorization error
     */
    private void updateObjectStatus(
            Context context, DSpaceObject dso, METSManifest manifest, PackageParameters params
    ) throws SQLException, AuthorizeException {
        if (dso == null) {
            log.warn("Unable to update the object status :: object is null");
            return;
        }
        // Only ITEM object could have 'withdrawn' flag; but not for workflow items
        if (dso.getType() == Constants.ITEM && !params.workflowEnabled()) {
            String manifestRecordStatus = manifest.getRecordStatus();
            if (manifestRecordStatus != null && manifestRecordStatus.equalsIgnoreCase("inactive")) {
                Item item = (Item)dso;
                itemService.withdraw(context, item);
                itemService.update(context, item);
                log.debug("Enable withdrawn status for " + item);
            }
        }
    }

    /**
     * Search ancestor identifiers (PID, Handle URI?, ...) and insert them into object metadata.
     *
     * @param context     DSpace context
     * @param dso         DSpace object to manage
     * @param manifest    The parse METS manifest
     */
    private void addAncestorIdentifier(
            Context context, DSpaceObject dso, METSManifest manifest, PackageParameters params
    ) throws SQLException {
        // FEDORA PID ---------------------------------------------------------
        if (dso.getType() == Constants.ITEM && !params.workflowEnabled()) { // Only applied for ITEM
            Item item = (Item) dso;
            String fedoraPid = manifest.getMets().getAttributeValue("ID");
            if (fedoraPid == null || fedoraPid.isEmpty()) {
                return;
            }
            fedoraPid = fedoraPid.replace("-", ":"); // ID attribute use dash, but we want to use the original pattern
            itemService.addMetadata(context, item, "fedora", "pid", null, null, fedoraPid);
        }
    }

    /**
     * Fix bitstream file name and extract potential descriptive metadata to
     * join to the bitstream.
     *
     * @param context  context
     * @param manifest METS manifest
     * @param bs       bitstream
     * @param mfile    element
     * @param params   package params
     *
     * @throws MetadataValidationException if validation error
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     */
    @Override
    public void finishBitstream(Context context, Bitstream bs, Element mfile, METSManifest manifest,
                                PackageParameters params)
            throws MetadataValidationException, SQLException, AuthorizeException, IOException {
        // First of all, call super method...
        super.finishBitstream(context, bs, mfile, manifest, params);

        // Removes `files/` to the bitstream filename.
        // This part represents the directory where stored the file into the METS archive and isn't relevant.
        if (bs.getName().startsWith("files/")) {
            bs.setName(context, bs.getName().replaceFirst("files/", ""));
        }

        // Try to find some additional metadata for this bitstream from METS Manifest DMDSec.
        // For each data found, add it into bitstream metadata
        if (transformFile == null) {
            log.debug("Unable to extract bitstream metadata : no stylesheet file defined.");
            return;
        }
        Element dmdSec = getFileDmdSection(manifest, mfile);
        if (dmdSec != null) {
            applyDim(context, extractBitstreamMetadata(dmdSec), bs);
        }
    }

    @Override
    public void finishObject(Context context, DSpaceObject dso, PackageParameters params)
            throws PackageValidationException, CrosswalkException, AuthorizeException, SQLException, IOException {
        // Legacy comments creation
        //   After loading, the legacy comments are present into bitstreams from "COMMENT" bundle.
        //   We need to extract each comment to create a corresponding `Comment` DSO into the system.
        if (dso instanceof Item) {
            createLegacyComment(context, (Item) dso);
        }
    }

    /**
     * Analyze the DMD section to find potential authority relations and add them into the DMD before crosswalk of this
     * DMD section.
     * In our case, we will try to find authority relations for:
     *   - publication authors (`ResearchProfile` relation based on authors identifiers and/or emails)
     *   - publication related journal (`Journal` relation based on journal identifiers and/or journal title)
     *   - publication related affiliation (`OrgUnit` relation based on affiliation institution and department name)
     *
     * @param context the Dspace application context
     * @param dmdSec the DMD section to prepare
     * @throws SQLException               if database error
     * @throws AuthorizeException         if authorization error
     */
    @Override
    public void prepareItemDmd(Context context, Element dmdSec) throws AuthorizeException, SQLException {
        findAuthorAuthorityRelation(context, dmdSec);
        findAffiliationAuthorityRelation(context, dmdSec);
        findJournalAuthorityRelation(context, dmdSec);
        log.debug("After preparing item DmdSec ::");
        log.debug(new XMLOutputter(Format.getPrettyFormat()).outputString(dmdSec));
    }

    // PRIVATE METHODS ========================================================
    /**
     * Find the dmdSec corresponding to a file from a METS Manifest
     *
     * @param manifest the METS manifest
     * @param mfile    The file node to analyze
     * @return the XML element corresponding to the dmdSec related to the file; `null` if not found
     * @throws MetadataValidationException if any error occurs when parsing the METS manifest.
     */
    private Element getFileDmdSection(METSManifest manifest, Element mfile) throws MetadataValidationException {
        String dmdSecID = mfile.getAttributeValue("DMDID");
        return (dmdSecID == null) ? null : Arrays.stream(manifest.getDmdElements(dmdSecID)).findFirst().orElse(null);
    }

    /**
     * Extract the bitstream metadata from a METS dmdSec using extraction stylesheet.
     *
     * @param dmdSec the dmdSec to analyze
     * @return a list of DIM field element (that could contain DIM fields, ...); at least an empty list.
     * @throws MetadataValidationException if any error occurs when parsing the METS manifest.
     */
    private List<Element> extractBitstreamMetadata(Element dmdSec) throws MetadataValidationException {
        Element rootElement = getRootElement(dmdSec);
        if (rootElement == null) {
            throw new MetadataValidationException("Bitstream metadata could only be extracted");
        }
        Transformer xform = getTransformer();
        try {
            JDOMResult result = new JDOMResult();
            xform.transform(new JDOMSource(rootElement), result);
            List<Content> contentList = result.getResult();
            // Transform List<Content> into List<Element>
            return contentList.stream()
                .filter(obj -> obj instanceof Element)
                .map(Element.class::cast)
                .collect(Collectors.toList());
        } catch (TransformerException e) {
            log.error("Error extracting bitstream metadata : " + e);
            return new ArrayList<>();
        }
    }

    /**
     * Extract the useful root XML element from a METS dmdSec
     *
     * @param dmdSec the METS dmdSec to analyze/
     * @return the root DC element or null if not found.
     * @throws MetadataValidationException if dmdSec element failed to be parsed.
     */
    private Element getRootElement(Element dmdSec)  throws MetadataValidationException {
        List<Element> mdc = dmdSec.getChildren();
        String exceptionPrefixMessage = "Cannot parse dmdSec[@ID=" + dmdSec.getAttributeValue("ID") + "] :: ";
        if (mdc.size() > 1) {
            throw new MetadataValidationException(exceptionPrefixMessage + "Only one mdWrap child is allowed");
        }
        Element mdWrap = dmdSec.getChild("mdWrap", METSManifest.metsNS);
        if (mdWrap == null) {
            throw new MetadataValidationException(exceptionPrefixMessage + "mdWrap child is required");
        }
        Element xmlData = mdWrap.getChild("xmlData", METSManifest.metsNS);
        if (xmlData == null) {
            throw new MetadataValidationException(exceptionPrefixMessage + "xmlData child is required");
        }
        return xmlData.getChildren().stream().findFirst().orElse(null);
    }

    /**
     * Initialize the transformation stylesheet from configured stylesheet file.
     *
     * @return transformer to use to extract bitstream metadata, or `null` if there was error initializing.
     */
    private Transformer getTransformer() {
        if (transformer == null || transformFile.lastModified() > transformerLastModified) {
            try {
                log.debug((transformer == null ? "Loading" : "Relaoding") + " XSLT stylesheet from "
                        + transformFile.toString());
                Source transformSource = new StreamSource(new FileInputStream(transformFile));
                TransformerFactory factory = TransformerFactory.newInstance();
                transformer = factory.newTransformer(transformSource);
                transformerLastModified = transformFile.lastModified();
            } catch (TransformerConfigurationException | FileNotFoundException e) {
                log.error("Failed to initialize DSpaceUCLouvainMETSIngester : " + e);
            }
        }
        return this.transformer;
    }

    /**
     * apply metadata values returned in DIM to the target bitstream.
     * @param context    the application context
     * @param dimList    the DIM element list to apply.
     * @param bitstream  the targeted bitstream where the metadata will be added.
     * @throws MetadataValidationException if any validation exception occurred
     * @throws SQLException if any database exception occurred
     */
    private static void applyDim(Context context, List<Element> dimList, Bitstream bitstream)
            throws MetadataValidationException, SQLException {
        for (Element elt : dimList) {
            if ("field".equals(elt.getName()) && DIM_NS.equals(elt.getNamespace())) {
                applyDimField(context, elt, bitstream);
            } else if ("dim".equals(elt.getName()) && DIM_NS.equals(elt.getNamespace())) {
                // if it's a <dim> container --> recursive magic !
                applyDim(context, elt.getChildren(), bitstream);
            } else {
                log.error("Got unexpected element in DIM list: " + elt);
                throw new MetadataValidationException("Got unexpected element in DIM list: " + elt);
            }
        }
    }
    private static void applyDimField(Context context, Element field, Bitstream bitstream)
            throws MetadataValidationException, SQLException {
        String schema = field.getAttributeValue("mdschema");
        String element = field.getAttributeValue("element");
        String qualifier = field.getAttributeValue("qualifier");
        String lang = field.getAttributeValue("lang");
        String authority = field.getAttributeValue("authority");
        String sconf = field.getAttributeValue("confidence");
        // SanityCheck: some XSL puts an empty string in qualifier,
        // change it to null, so we match the unqualified DC field:
        if (qualifier != null && qualifier.isEmpty()) {
            qualifier = null;
        }
        // Find the metadata field. If the field doesn't exist, raise an exception.
        MetadataField metadataField = metadataFieldService.findByElement(context, schema, element, qualifier);
        if (metadataField == null) {
            String fieldName = schema + '.' + element;
            if (qualifier != null) {
                fieldName += '.' + qualifier;
            }
            throw new MetadataValidationException("Unable to find metadata field for " + fieldName);
        }
        // Add the metadata
        if ((authority != null && !authority.isEmpty()) || (sconf != null && !sconf.isEmpty())) {
            int confidence = (sconf != null && !sconf.isEmpty()) ? Choices.getConfidenceValue(sconf) : Choices.CF_UNSET;
            bitstreamService.addMetadata(
                    context,
                    bitstream,
                    metadataField,
                    lang,
                    field.getText(),
                    authority,
                    confidence
            );
        } else {
            bitstreamService.addMetadata(
                    context,
                    bitstream,
                    metadataField,
                    lang,
                    field.getText()
            );
        }
    }

    /**
     * Add additional metadata when an item is replaced/restored.
     * As all previous metadata previously cleared, we would add some specific technical metadata.
     * @param context The DSpace application context
     * @param item The item to update
     * @throws SQLException for any database exception
     */
    private void populateMetadata(Context context, Item item) throws SQLException {
        DCDate now = DCDate.getCurrent();
        // If the item doesn't have a date.accessioned, set it to today
        String mv = itemService.getMetadataFirstValue(item, "dc", "date", "accessioned", Item.ANY);
        if (StringUtils.isBlank(mv)) {
            itemService.addMetadata(context, item, "dc", "date", "accessioned", null, now.toString());
        }
        // If the item doesn't have a date.available (created for UCLouvain), set it to today
        String dateAvailable = itemService.getMetadataFirstValue(item, "dc", "date", "available", Item.ANY);
        if (StringUtils.isBlank(dateAvailable)) {
            itemService.addMetadata(context, item, "dc", "date", "available", null, now.toString());
        }
        // Record that the item was restored/replaced
        String provDescription = "Replacement into DSpace on " + now + " (GMT).";
        itemService.addMetadata(context, item, "dc", "description", "provenance", "en", provDescription);
    }

    // COMMENTS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Allows creating comments from a legacy source.
     *   During the SIP ingestion, comments are provided and stored into bitstreams stored into "COMMENT" bundle.
     *   We need to read these XML bitstream to extract all comments and create related DSpace comments
     *
     * @param context the dspace application context
     * @param item the related item
     * @throws SQLException if any database exception occurred
     */
    private void createLegacyComment(Context context, Item item) throws SQLException {
        commentService.deleteAllItemComments(context, item);
        // Get all possible bitstreams containing comments
        List<Bitstream> commentBitstreams = item.getBundles("COMMENT").stream()
                .flatMap(bundle -> bundle.getBitstreams().stream())
                .collect(Collectors.toList());
        // Extract all comments from
        List<LegacyComment> legacyComments = commentBitstreams.stream()
                .flatMap(bitstream -> loadLegacyComments(context, bitstream).stream())
                .collect(Collectors.toList());
        for (LegacyComment comment : legacyComments) {
            commentService.create(context, item, comment.getWriter(), comment.getContent());
        }

        // We can now safely delete all "comment" bitstream. We can also delete the related bundles
        commentBitstreams.forEach(b -> safeDeleteDSO(context, b));
        item.getBundles("COMMENT").forEach(b -> safeDeleteDSO(context, b));
    }

    private void safeDeleteDSO(Context context, DSpaceObject dso) {
        try {
            DSpaceObjectService<DSpaceObject> service = ContentServiceFactory.getInstance().getDSpaceObjectService(dso);
            service.delete(context, dso);
        } catch (Exception e) {
            log.error("Error deleting comment " + dso.getClass().getName() + "@" + dso.getID(), e);
        }
    }

    // AUTHORITY LINKING ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Search about a possible authority linking for publication authors.
     * The authority search could be done using OrcidID, FGS or email; not on author name (too ambiguous).
     * @param context the Dspace application context
     * @param dmdSec the root DMD section containment the MODS metadata
     * @throws AuthorizeException for authorization error
     * @throws SQLException for database error
     */
    private void findAuthorAuthorityRelation(Context context, Element dmdSec) throws AuthorizeException, SQLException {
        String xpathQuery =
                ".//mods:mods/mods:name[@type='personal' and mods:role/mods:roleTerm[@type='text'] != 'supervisor']";
        for (Element authorElement : xpathElements(dmdSec, xpathQuery)) {
            String authorName = xpathGetValue(authorElement, "mods:namePart");
            String email = xpathGetValue(authorElement, "mods:nameIdentifier[@type='email']");
            String orcid = xpathGetValue(authorElement, "mods:nameIdentifier[@type='orcid']");
            String fgs = xpathGetValue(authorElement, "mods:nameIdentifier[@type='fgs']");
            log.debug("Try to find corresponding authority for author [" + authorName + "] ....");

            Map<String, String> identifiers = new HashMap<>();
            if (email != null) {
                identifiers.put("person.email", email);
                identifiers.put("person.email.official", email);
            }
            if (orcid != null) {
                identifiers.put("person.identifier.orcid", orcid.replace("https://orcid.org/", ""));
            }
            if (fgs != null) {
                identifiers.put("person.identifier.fgs", fgs);
            }
            if (!identifiers.isEmpty()) {
                String pretty = identifiers.entrySet().stream()
                    .map(entry -> entry.getKey() + ":" + entry.getValue())
                    .collect(Collectors.joining(", ", "[", "]"));
                log.debug("  * Identifiers are " + pretty);
                Item profile = researcherProfileService.findByIdentifier(context, identifiers);
                if (profile != null) {
                    log.debug("  * Matching authority found: " + profile.getID());
                    updateAuthorElementValues(authorElement, profile);
                    authorElement.setAttribute("authority", profile.getID().toString());
                } else {
                    log.debug("  * No matching authority for this author");
                }
            } else {
                log.debug("  * No identifiers found for this author. No authority link possible");
            }
        }
    }
    private void updateAuthorElementValues(Element authorElement, Item profile) {
        // Updates author name with accepted values stored into "dc.title"
        //   'mods:namePart' should always exist into the MODS when describing an author
        String acceptedAuthorName = itemService.getMetadataFirstValue(profile, "dc", "title", null, null);
        if (StringUtils.isNotBlank(acceptedAuthorName)) {
            xpathElements(authorElement, "./mods:namePart")
                .stream()
                .findFirst()
                .ifPresent(authorNameElement -> authorNameElement.setText(acceptedAuthorName));
        }
        // Updates ORCID-ID with accepted values stored into "person.identifier.orcid"
        //    If an accepted value is available into the researcher profile:
        //      * any existing data from an external source must be updated (logically, both values should point to the
        //        same orcid profile).
        //      * If the external source doesn't provide any data, create the new tag to insert the accepted orcid value
        String acceptedOrcidID = itemService.getMetadataFirstValue(profile, "person", "identifier", "orcid", null);
        if (StringUtils.isNotBlank(acceptedOrcidID)) {
            Element orcidTag = xpathElements(authorElement, "mods:nameIdentifier[@type='orcid']")
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Element tag = new Element("nameIdentifier", modsNS);
                    authorElement.addContent(tag);
                    return tag;
                });
            orcidTag.setText(acceptedOrcidID);
        }
    }

    /**
     * Search about possible authority linking for publication affiliations.
     * The authority search could be done using affiliation institution & department name.
     * @param context the Dspace application context
     * @param dmdSec the root DMD section containment the MODS metadata
     * @throws SQLException for database error
     */
    private void findAffiliationAuthorityRelation(Context context, Element dmdSec) throws SQLException {
        String xpathQuery = ".//mods:mods/mods:relatedItem[@otherType='affiliation']";
        String instXpathquery = "mods:name[@type='corporate']";
        for (Element affiliationElement : xpathElements(dmdSec, xpathQuery)) {
            Element instElement = xpathElements(affiliationElement, instXpathquery).stream().findFirst().orElse(null);
            if (instElement == null) {
                log.warn("Find an affiliation without related institution :: skip it");
                continue;
            }
            String instAcronym = instElement.getText();
            String entityName = xpathGetValue(affiliationElement, "mods:titleInfo/mods:title");
            OrgUnit matchingInst = orgUnitService.findByName(context, instAcronym, null, null, null);
            if (matchingInst != null) {
                log.debug("  * Affiliation [" + instAcronym + "] link to authority " + matchingInst.getID());
                instElement.setAttribute("authority", matchingInst.getID().toString());
            } else {
                log.debug("  * Affiliation [" + instAcronym + "] not match any authority");
            }
            OrgUnit matchingEntity = orgUnitService.findByName(context, instAcronym, null, null, entityName);
            if (matchingEntity != null) {
                log.debug("  * Affiliation [" + entityName + "] link to authority " + matchingEntity.getID());
                affiliationElement.setAttribute("authority", matchingEntity.getID().toString());
            } else {
                log.debug("  * Affiliation [" + entityName + "] not match any authority");
            }
        }
    }

    /**
     * Search about possible authority linking for publication journal.
     * The authority search could be done using related journal ISSN/e-ISSN or journal title
     * @param context the Dspace application context
     * @param dmdSec the root DMD section containment the MODS metadata
     */
    private void findJournalAuthorityRelation(Context context, Element dmdSec) {
        String xpathQuery = ".//mods:mods/mods:relatedItem[@otherType='host' and mods:genre/text() = 'journal']";
        Element journalElement = xpathElements(dmdSec, xpathQuery).stream().findFirst().orElse(null);
        if (journalElement != null) {
            String journalTitle = xpathGetValue(journalElement, "mods:titleInfo/mods:title");
            String issnValue = xpathGetValue(journalElement, "mods:identifier[@type='issn']");
            if (StringUtils.isNotBlank(issnValue)) {
                Journal journal = journalService.findByIssn(context, issnValue);
                if (journal != null) {
                    log.debug("  * Journal [" + journalTitle + "] link to authority " + journal.getID());
                    updateJournalElementValue(journalElement, journal);
                    journalElement.setAttribute("authority", journal.getID().toString());
                    return;
                }
            }
            String eissnValue = xpathGetValue(journalElement, "mods:identifier[@type='eissn']");
            if (StringUtils.isNotBlank(eissnValue)) {
                Journal journal = journalService.findByEissn(context, eissnValue);
                if (journal != null) {
                    log.debug("  * Journal  [" + journalTitle + "] link to authority " + journal.getID());
                    updateJournalElementValue(journalElement, journal);
                    journalElement.setAttribute("authority", journal.getID().toString());
                    return;
                }
            }
            if (StringUtils.isNotBlank(journalTitle)) {
                Journal journal = journalService.findByTitle(context, journalTitle);
                if (journal != null) {
                    log.debug("  * Journal [" + journalTitle + "] link to authority " + journal.getID());
                    updateJournalElementValue(journalElement, journal);
                    journalElement.setAttribute("authority", journal.getID().toString());
                    return;
                }
            }
            log.debug("  * Journal [" + journalTitle + "] not match any authority");
        }
    }
    private void updateJournalElementValue(Element journalElement, Journal journal) {
        // DEV NOTES :: We never override the original `peer-review` metadata.
        //   Sometime, in legacy metadata, user chose to manually update this value (despite KB data).
        //   Keeping this legacy data, will not override this choice despite authority metadata

        // Updates journal title with accepted values stored into journal "dc.title" metadata
        //   'mods:titleInfo/mods:title' should always exist into the MODS when describing a journal
        String acceptedTitle = journal.getTitle();
        if (StringUtils.isNotBlank(acceptedTitle)) {
            xpathElements(journalElement, "./mods:titleInfo/mods:title")
                .stream()
                .findFirst()
                .ifPresent(authorNameElement -> authorNameElement.setText(acceptedTitle));
        }
        // Update journal identifiers with accepted values (dc.identifier.[e]issn)
        //    Force update or create MODS tags with accepted values if exists.
        String acceptedISSN = journal.getIdentifier(Journal.ISSN_IDENTIFIER);
        if (StringUtils.isNotBlank(acceptedISSN)) {
            Element issnTag = xpathElements(journalElement, "mods:identifier[@type='issn']")
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Element newTag = new Element("identifier", modsNS).setAttribute("type", "issn");
                    journalElement.addContent(newTag);
                    return newTag;
                });
            issnTag.setText(acceptedISSN);
        }
        String acceptedEISSN = journal.getIdentifier(Journal.EISSN_IDENTIFIER);
        if (StringUtils.isNotBlank(acceptedEISSN)) {
            Element eissnTag = xpathElements(journalElement, "mods:identifier[@type='e-issn']")
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Element newTag = new Element("identifier", modsNS).setAttribute("type", "e-issn");
                    journalElement.addContent(newTag);
                    return newTag;
                });
            eissnTag.setText(acceptedEISSN);
        }
        // Update editor name/place with accepted values (dc.publisher[.location])
        //    Force update or create MODS tags with accepted values if exists.
        String editorName = journal.getPublisher();
        String editorLocation = journal.getPublisherLocation();
        if (StringUtils.isNotBlank(editorName) || StringUtils.isNotBlank(editorLocation)) {
            Element originInfo = xpathElements(journalElement, "./mods:originInfo")
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Element newTag = new Element("originInfo", modsNS);
                    journalElement.addContent(newTag);
                    return newTag;
                });
            if (StringUtils.isNotBlank(editorName)) {
                Element editorNameTag = xpathElements(originInfo, "./mods:publisher")
                    .stream()
                    .findFirst()
                    .orElseGet(() -> {
                        Element newTag = new Element("publisher", modsNS);
                        originInfo.addContent(newTag);
                        return newTag;
                    });
                editorNameTag.setText(editorName);
            }
            if (StringUtils.isNotBlank(editorLocation)) {
                Element editorLocationTag = xpathElements(originInfo, "./mods:place")
                        .stream()
                        .findFirst()
                        .orElseGet(() -> {
                            Element newTag = new Element("place", modsNS);
                            originInfo.addContent(newTag);
                            return newTag;
                        });
                editorLocationTag.setText(editorLocation);
            }
        }
    }

    private String xpathGetValue(Element root, String xpathQuery) {
        Element result = XPathFactory.instance()
            .compile(xpathQuery, Filters.element(), null, modsNS)
            .evaluateFirst(root);
        return (result != null)
            ? result.getText()
            : null;
    }
    private List<Element> xpathElements(Element root, String xpathQuery) {
        return XPathFactory.instance()
            .compile(xpathQuery, Filters.element(), null, modsNS)
            .evaluate(root);
    }


}