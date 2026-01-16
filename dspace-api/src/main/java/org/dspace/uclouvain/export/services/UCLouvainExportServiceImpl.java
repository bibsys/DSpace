/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.services;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.ItemExportCrosswalk;
import org.dspace.content.integration.crosswalks.StreamDisseminationCrosswalkMapper;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.exceptions.AuthorNotFoundException;
import org.dspace.uclouvain.exceptions.CrosswalkNotFoundException;
import org.dspace.uclouvain.export.result.ExportResult;
import org.dspace.uclouvain.export.result.TempFileExportResult;
import org.dspace.uclouvain.export.utils.FNRSExportUtils;
import org.dspace.uclouvain.services.UCLouvainProfileService;
import org.springframework.beans.factory.annotation.Autowired;

public class UCLouvainExportServiceImpl implements UCLouvainExportService {

    @Autowired
    protected StreamDisseminationCrosswalkMapper crosswalkMapper;
    @Autowired
    protected SearchService searchService;
    @Autowired
    protected ItemService itemService;
    @Autowired
    protected UCLouvainProfileService uclouvainProfileService;

    private final Logger logger = LogManager.getLogger(UCLouvainExportServiceImpl.class);
    private static final String CROSSWALK_SEPARATOR = "-";
    // TODO: Change to correct crosswalk name once created.
    private static final String FWB_CROSSWALK_KEY = "publication-fwb-pdf";
    private static final String FNRS_CROSSWALK_KEY = "publication-fnrs-pdf";
    private static final String FNRS_DOCUMENT_TITLE = "Publications de\"%s\"";
    private static final String FWB_DOCUMENT_TITLE = "Publications de \"%s\"";

    public ExportResult getExportResult(Context context, String style, String format, String query)
        throws CrosswalkNotFoundException, SearchServiceException, CrosswalkException {

        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(style, format);
        Iterator<Item> publications = findPublicationsFromQuery(context, query);
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }

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
        return findPublications(context, query, fqs).iterator();
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
        Stream<Item> unfilteredPublications = findPublications(context, query, fqs);
        // Filter to only keep FNRS valid publications.
        return unfilteredPublications.filter(publicationItem -> {
            try {
                return FNRSExportUtils.isFNRSValid(authorId, PublicationFactory.build(publicationItem));
            } catch (Exception e) {
                logger.warn(
                    "Skipping publication {} due to FNRS validation error",
                    publicationItem.getID(),
                    e
                );
                return false;
            }
        }).iterator();
    }

    private ItemExportCrosswalk findItemExportCrosswalk(String crosswalkId) throws CrosswalkNotFoundException {
        StreamDisseminationCrosswalk crosswalk = findCrosswalk(crosswalkId);
        // DEV_NOTE: We have to cast to a ItemExportCrosswalk object to use getFilename() and getMimeType().
        if (crosswalk == null || !(crosswalk instanceof ItemExportCrosswalk)) {
            logger.warn("Could not find a crosswalk for given id '{}'", crosswalkId);
            throw new CrosswalkNotFoundException("Unsupported style or format");
        }
        return (ItemExportCrosswalk) crosswalk;
    }

    private ItemExportCrosswalk findItemExportCrosswalk(String style, String format) throws CrosswalkNotFoundException {
        return findItemExportCrosswalk(parseCrosswalkID(style, format));
    }

    private String parseCrosswalkID(String style, String format) {
        return style + CROSSWALK_SEPARATOR + format;
    }

    private StreamDisseminationCrosswalk findCrosswalk(String id) {
        return crosswalkMapper.getByType(id);
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
        Item author = uclouvainProfileService.findByIdentifiers(context, authorUUID, authorFGS);
        if (author == null) {
            throw new AuthorNotFoundException("Could not find any matching author for given identifiers.");
        }
        return author;
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
        query = "%s:'%s'".formatted(field, value);
        return findPublications(context, query, new HashMap<>()).iterator();
    }

    /**
     * Find all publication items matching the given query and filter queries.
     * TODO: Improve this logic to handle more params (sort, filters...). It will be better to externalize this code.
     * @param context The current DSpace context.
     * @param query The main query to match.
     * @param filterQueries Additional filter queries to match.
     * @return A stream of all found publications based on the given query.
     * @throws SearchServiceException
     */
    private Stream<Item> findPublications(
        Context context, String query, Map<String, String> filterQueries
    ) throws SearchServiceException {
        DiscoverQuery dq = new DiscoverQuery();
        dq.addDSpaceObjectFilter(IndexableItem.TYPE);
        dq.setQuery(query);
        dq.setMaxResults(50000);
        filterQueries.entrySet().forEach((Entry<String, String> entry) -> {
            dq.addFilterQueries("%s:\"%s\"".formatted(entry.getKey(), entry.getValue()));
        });
        DiscoverResult searchResult = searchService.search(context, dq);
        return searchResult.getIndexableObjects()
            .stream()
            .map(indexableObject -> ((IndexableItem) indexableObject).getIndexedObject());
    }
}
