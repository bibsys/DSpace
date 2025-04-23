/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.packager;

import static org.dspace.content.crosswalk.XSLTCrosswalk.DIM_NS;
import static org.dspace.core.Constants.CONTENT_BUNDLE_NAME;
import static org.dspace.uclouvain.constants.AccessConditions.EMBARGO;
import static org.dspace.uclouvain.constants.AccessConditions.OPEN_ACCESS;
import static org.dspace.uclouvain.content.utils.CommentUtils.loadLegacyComments;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
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
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.content.Comment;
import org.dspace.uclouvain.content.LegacyComment;
import org.dspace.uclouvain.content.service.CommentService;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.dspace.workflow.WorkflowException;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.transform.JDOMResult;
import org.jdom2.transform.JDOMSource;

public class DSpaceUCLouvainMETSIngester extends DSpaceMETSIngester {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AbstractMETSIngester.class);
    private static final String bitstreamExtractorStylesheetConfigKey =
            "uclouvain.ingester.bitstreamMetadataExtractor.stylesheet";
    private static final MetadataFieldService metadataFieldService =
            ContentServiceFactory.getInstance().getMetadataFieldService();
    private static final BitstreamService bitstreamService =
            ContentServiceFactory.getInstance().getBitstreamService();
    private final CommentService commentService = UCLouvainServiceFactory.getInstance().getCommentService();
    private final UCLouvainResourcePolicyService policyService =
            UCLouvainServiceFactory.getInstance().getResourcePolicyService();

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
     * @param context  DSpace context
     * @param parent   Parent DSpace Object
     * @param manifest the parsed METS Manifest
     * @param pkgFile  the full package file (which may include content files if a zip)
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
        context.turnOffAutomaticCommentCreation();
        DSpaceObject dso = super.ingestObject(context, parent, manifest, pkgFile, params, license);
        this.addAncestorIdentifier(context, dso, manifest, params);
        this.updateObjectStatus(context, dso, manifest, params);
        context.restoreAutomaticCommentCreation();
        return dso;
    }

    @Override
    public DSpaceObject replace(Context context, DSpaceObject dsoToReplace, File pkgFile, PackageParameters params)
            throws PackageValidationException, CrosswalkException, AuthorizeException, SQLException, IOException,
                   WorkflowException {
        DSpaceObject dso = super.replace(context, dsoToReplace, pkgFile, params);
        if (dso.getType() == Constants.ITEM) {
            updateOwningCollection(context, (Item) dso, pkgFile, params);
            if (params.useCollectionTemplate()) {
                applyCollectionTemplate(context, (Item) dso);
            }
        }
        return dso;
    }

    // PRIVATE METHODS =================================================================================================

    /**
     * Analyze the manifest to determine if the object must be moved from the current collection to new one.
     *
     * @param context DSpace context
     * @param item    Dspace Item to update
     * @param pkgFile The full package file (which may include content files if a zip)
     * @param params  Parameters passed to METSIngester
     */
    private void updateOwningCollection(Context context, Item item, File pkgFile, PackageParameters params)
        throws PackageValidationException, SQLException, MetadataValidationException, AuthorizeException, IOException {
        METSManifest manifest = parsePackage(context, pkgFile, params);
        DSpaceObject mColl = getParentObject(context, manifest);
        DSpaceObject cColl = itemService.getParentObject(context, item);
        if (mColl.getType() != Constants.COLLECTION || cColl.getType() != Constants.COLLECTION) {
            log.warn("Collection replacement will only work with COLLECTION objects");
            log.warn("  --> current  :: (" + Constants.typeText[cColl.getType()] + ") " + cColl.getID());
            log.warn("  --> manifest :: (" + Constants.typeText[mColl.getType()] + ") " + mColl.getID());
        }
        itemService.move(context, item, (Collection) cColl, (Collection) mColl);
    }

    /**
     * Apply collection template to item
     *
     * @param context DSpace context
     * @param item Dspace item to update
     */
    private void applyCollectionTemplate(Context context, Item item) throws SQLException {
        Collection collection = item.getOwningCollection();
        Item templateItem = collection.getTemplateItem();
        if (templateItem != null) {
            List<MetadataValue> mdValues = itemService.getMetadata(templateItem, Item.ANY, Item.ANY,Item.ANY, Item.ANY);
            for (MetadataValue mdValue : mdValues) {
                MetadataField mdField = mdValue.getMetadataField();
                MetadataSchema mdSchema = mdField.getMetadataSchema();
                List<MetadataValue> itemMetadata = itemService.getMetadata(
                        item, mdSchema.getName(), mdField.getElement(), mdField.getQualifier(), Item.ANY);
                if (itemMetadata.isEmpty()) {
                    itemService.addMetadata(
                            context, item,
                            mdSchema.getName(), mdField.getElement(), mdField.getQualifier(), mdValue.getLanguage(),
                            mdValue.getValue(), mdValue.getAuthority(), mdValue.getConfidence()
                    );
                }
            }
        }
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
        //   We also create a new comment to store access status of loaded bitstream
        if (dso instanceof Item) {
            createLegacyComment(context, (Item) dso);
            createBitstreamAccessComment(context, (Item) dso);
        }
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
            Comment newComment = commentService.create(context, item, comment.getWriter(), comment.getContent());
            commentService.forceCreatedDate(context, newComment, comment.getCreated());
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

    /**
     * Create a comment to store the initial bitstream access condition after loading
     *
     * @param context the application context
     * @param item the related item
     */
    private void createBitstreamAccessComment(Context context, Item item) {
        item.getBundles(CONTENT_BUNDLE_NAME).stream()
            .flatMap(bundle -> bundle.getBitstreams().stream())
            .forEach(bitstream -> this.createInitialAccessBitstreamComment(context, item, bitstream));
    }

    private void createInitialAccessBitstreamComment(Context context, Item item, Bitstream bitstream) {
        try {
            ResourcePolicy masterPolicy = policyService.getMasterPolicy(policyService.find(context, bitstream));
            String accessStatus = (masterPolicy != null) ? masterPolicy.getRpName() : OPEN_ACCESS;
            if (masterPolicy != null && masterPolicy.getRpName().equalsIgnoreCase(EMBARGO)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                accessStatus += " until (" + dateFormat.format(masterPolicy.getStartDate()) + ")";
            }
            String commentContent = "Bitstream@" + bitstream.getID() + " with name \"" + bitstream.getName() + "\" :: ";
            commentContent += "Legacy accessCondition is " + accessStatus + "\n";
            commentService.create(context, item, context.getCurrentUser(), commentContent);
        } catch (Exception e) {
            log.warn("Error generating comment :: " + e.getMessage(), e);
        }
    }
}