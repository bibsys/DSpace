/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.ItemExportCrosswalk;
import org.dspace.content.integration.crosswalks.StreamDisseminationCrosswalkMapper;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.uclouvain.core.model.OrgUnit;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.exceptions.AffiliationNotFoundException;
import org.dspace.uclouvain.exceptions.AuthorNotFoundException;
import org.dspace.uclouvain.exceptions.CrosswalkNotFoundException;
import org.dspace.uclouvain.export.result.ExportResult;
import org.dspace.uclouvain.export.result.TempFileExportResult;
import org.dspace.uclouvain.export.utils.FNRSExportUtils;
import org.dspace.uclouvain.services.OrgUnitService;
import org.dspace.uclouvain.services.PublicationService;
import org.dspace.uclouvain.services.UCLouvainProfileService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service in charge of export operations for UCLouvain custom export API.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 * @author Renaud Michotte <renaud.michotee@uclouvain.be>
 */
public class UCLouvainExportServiceImpl implements UCLouvainExportService {

    @Autowired
    protected StreamDisseminationCrosswalkMapper crosswalkMapper;
    @Autowired
    protected UCLouvainProfileService uclouvainProfileService;
    @Autowired
    protected OrgUnitService orgUnitService;
    @Autowired
    protected PublicationService publicationService;

    private final Logger logger = LogManager.getLogger(UCLouvainExportServiceImpl.class);
    // TODO: Change to correct crosswalk name once created.
    private static final String FWB_CROSSWALK_KEY = "publication-fwb-pdf";
    private static final String FNRS_CROSSWALK_KEY = "publication-fnrs-pdf";
    private static final String FNRS_DOCUMENT_TITLE = "Publications de\"%s\"";
    private static final String FWB_DOCUMENT_TITLE = "Publications de \"%s\"";

    // Custom export

    public ExportResult getExportResult(Context context, String crosswalkName, String query)
        throws CrosswalkNotFoundException, SearchServiceException, CrosswalkException {

        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(crosswalkName);
        Iterator<Item> publications = findPublicationsFromQuery(context, query);
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }

    // Bibliographies exports

    public ExportResult getAuthorFWBBibliography(Context context, String authorUUID, String authorFGS)
        throws AuthorNotFoundException, SearchServiceException, CrosswalkNotFoundException, CrosswalkException {
        Item author = findAuthor(context, authorFGS, authorUUID);
        Iterator<Item> publications = findFWBPublications(context, author.getID().toString());
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(FWB_CROSSWALK_KEY);
        itemCrosswalk.addTransformerParameter("highlightText", author.getName());
        itemCrosswalk.addTransformerParameter("documentTitle", FWB_DOCUMENT_TITLE.formatted(author.getName()));
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }

    public ExportResult getAuthorFNRSBibliography(Context context, String authorUUID, String authorFGS)
        throws AuthorNotFoundException, SearchServiceException, CrosswalkNotFoundException, CrosswalkException {
        Item author = findAuthor(context, authorFGS, authorUUID);
        Iterator<Item> publications = findFNRSPublications(context, author.getID().toString());
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(FNRS_CROSSWALK_KEY);
        itemCrosswalk.addTransformerParameter("highlightText", author.getName());
        itemCrosswalk.addTransformerParameter("documentTitle", FNRS_DOCUMENT_TITLE.formatted(author.getName()));
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }

    // 'Find by' exports

    public ExportResult findByAuthor(
        Context context, String authorFGS, String authorUUID, String authorName, String crosswalk
    ) throws CrosswalkException, CrosswalkNotFoundException, AuthorNotFoundException, SearchServiceException {
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(crosswalk);
        List<Item> authors = findAuthors(context, authorUUID, authorFGS, authorName);
        Iterator<Item> publications = publicationService.findByAuthors(context, authors)
            .map(Publication::getItem)
            .iterator();
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }

    public ExportResult findByAffiliation(
        Context context, String affiliationUUID, String affiliationName, String crosswalk
    ) throws CrosswalkException, CrosswalkNotFoundException, AffiliationNotFoundException, SearchServiceException {
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(crosswalk);
        List<OrgUnit> affiliations = findAffiliations(context, affiliationUUID, affiliationName);
        Iterator<Item> publications = publicationService.findByAffiliations(context, affiliations)
            .map(Publication::getItem)
            .iterator();
        return new TempFileExportResult(context, itemCrosswalk, publications);
    };

    public ExportResult findByFunding(
        Context context, String organization, String program, String crosswalk
    ) throws CrosswalkException, CrosswalkNotFoundException, SearchServiceException {
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(crosswalk);
        Iterator<Item> publications = publicationService.findByFunding(context, organization, program)
            .map(Publication::getItem)
            .iterator();
        return new TempFileExportResult(context, itemCrosswalk, publications);
    };

    // PRIVATE METHODS -------------------------------------------------------------------------------------------------

    /**
     * Find all FWB valid publications for a given author.
     * @param context The current DSpace context.
     * @param authorId The id of the author.
     * @return An iterator of all FWB valid publication items of the given author.
     * @throws SearchServiceException
     */
    private Iterator<Item> findFWBPublications(Context context, String authorId) throws SearchServiceException {
        String query = String.format("isAuthorOfPublication:\"%s\"", authorId);
        Map<String, String> fqs = new HashMap<>();
        fqs.put("search.entitytype", Publication.ENTITY_TYPE);
        fqs.put("fwbCompliant_b", "true");
        return publicationService.findPublications(context, query, fqs)
            .map(Publication::getItem)
            .iterator();
    }

    /**
     * Find all FNRS valid publications of a given author.
     * The publications have to be valid (FNRSValid) and also the author has to be a valid FNRS author (role validation)
     * 
     * @param context The current DSpace context.
     * @param authorId The UUID of the author to get publications of.
     * @return An iterator of all FNRS valid publications for the given author.
     * @throws SearchServiceException
     */
    private Iterator<Item> findFNRSPublications(Context context, String authorId) throws SearchServiceException {
        // First find unfiltered publications.
        String query = String.format("isAuthorOfPublication:\"%s\"", authorId);
        Map<String, String> fqs = new HashMap<>();
        fqs.put("search.entitytype", Publication.ENTITY_TYPE);
        fqs.put("fnrsValid_b", "true");
        Stream<Publication> unfilteredPublications = publicationService.findPublications(context, query, fqs);
        // Filter to only keep FNRS valid publications.
        return unfilteredPublications.filter(publication -> {
            try {
                return FNRSExportUtils.isFNRSValid(authorId, publication);
            } catch (Exception e) {
                logger.warn(
                    "Skipping publication {} due to FNRS validation error",
                    publication.getID(),
                    e
                );
                return false;
            }
        }).map(Publication::getItem).iterator();
    }

    /**
     * Retrieve an ItemCrosswalk for the given crosswalk id.
     * @throws CrosswalkNotFoundException If no crosswalk could be found for the given id.
     */
    private ItemExportCrosswalk findItemExportCrosswalk(String crosswalkId) throws CrosswalkNotFoundException {
        StreamDisseminationCrosswalk crosswalk = crosswalkMapper.getByType(crosswalkId);
        // DEV_NOTE: We have to cast to a ItemExportCrosswalk object to use getFilename() and getMimeType().
        if (crosswalk == null || !(crosswalk instanceof ItemExportCrosswalk)) {
            logger.warn("Could not find a crosswalk for given id '{}'", crosswalkId);
            throw new CrosswalkNotFoundException("'%s' crosswalk not found".formatted(crosswalkId));
        }
        return (ItemExportCrosswalk) crosswalk;
    }

    /**
     * Find an author Item using one of the provided identifier (FGS or UUID).
     * @param context The current DSpace context.
     * @param authorFGS The FGS identifier of the author.
     * @param authorUUID The UUID of the author item.
     * @return An item corresponding to the author profile.
     * @throws Exception Throws and exception if no author item could be found using the provided identifiers.
     */
    private Item findAuthor(Context context, String authorFGS, String authorUUID) throws AuthorNotFoundException {
        Item author = uclouvainProfileService.findByIdentifiers(context, authorUUID, authorFGS, null);
        if (author == null) {
            throw new AuthorNotFoundException("Could not find any matching author for given identifiers.");
        }
        return author;
    }

    private List<Item> findAuthors(
        Context context, String authorFGS, String authorUUID, String authorName
    ) throws AuthorNotFoundException {
        if (authorFGS != null || authorUUID != null) {
            return Arrays.asList(this.findAuthor(context, authorFGS, authorUUID));
        }
        if (authorName != null) {
            List<Item> authors = uclouvainProfileService.findByName(context, authorName);
            if (!authors.isEmpty()) {
                return authors;
            }
        }
        throw new AuthorNotFoundException("Could not find any matching author for given identifiers or name.");
    }

    private List<OrgUnit> findAffiliations(
        Context context, String affiliationUUID, String affiliationName
    ) throws AffiliationNotFoundException {
        if (affiliationUUID != null) {
            OrgUnit affiliationByID =  orgUnitService.findByIdentifier(context, affiliationUUID);
            if (affiliationByID != null) {
                return Arrays.asList(affiliationByID);
            }
        }
        if (affiliationName != null) {
            List<OrgUnit> affiliations = orgUnitService.findByName(context, affiliationName);
            if (!affiliations.isEmpty()) {
                return affiliations;
            }
        }
        throw new AffiliationNotFoundException(
            "Could not find any matching affiliation for given identifiers or name.");
    }

    /**
     * Find all publication items matching the given query.
     * @param context The current DSpace context.
     * @param query The query to use in order to find the publications.
     * @return An iterator of publication items.
     * @throws SearchServiceException
     */
    private Iterator<Item> findPublicationsFromQuery(Context context, String query) throws SearchServiceException {
        String[] tokens = query.split("::");
        if (tokens.length != 2) {
            throw new IllegalArgumentException("Invalid query '%s'".formatted(query));
        }
        String field = tokens[0];
        String value = tokens[1];
        query = "%s:\"%s\"".formatted(field, value);
        return publicationService.findPublications(context, query, new HashMap<>())
            .map(Publication::getItem)
            .iterator();
    }
}
