/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.services;

import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.ItemExportCrosswalk;
import org.dspace.content.integration.crosswalks.StreamDisseminationCrosswalkMapper;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchServiceException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.exceptions.AffiliationNotFoundException;
import org.dspace.uclouvain.exceptions.AuthorNotFoundException;
import org.dspace.uclouvain.exceptions.CrosswalkNotFoundException;
import org.dspace.uclouvain.export.result.ExportResult;
import org.dspace.uclouvain.export.result.TempFileExportResult;
import org.dspace.uclouvain.export.utils.FNRSExportUtils;
import org.dspace.uclouvain.services.PublicationService;
import org.dspace.uclouvain.services.UCLouvainProfileService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service in charge of export operations for UCLouvain custom export API.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class UCLouvainExportServiceImpl implements UCLouvainExportService {

    @Autowired
    protected StreamDisseminationCrosswalkMapper crosswalkMapper;
    @Autowired
    protected UCLouvainProfileService uclouvainProfileService;
    @Autowired
    protected PublicationService publicationService;

    private final Logger logger = LogManager.getLogger(UCLouvainExportServiceImpl.class);
    // TODO: Change to correct crosswalk name once created.
    private static final String FWB_CROSSWALK_KEY = "publication-fwb-pdf";
    private static final String FNRS_CROSSWALK_KEY = "publication-fnrs-pdf";
    private static final String FNRS_DOCUMENT_TITLE = "Publications de \"%s\"";
    private static final String FWB_DOCUMENT_TITLE = "Publications de \"%s\"";

    // BIBLIOGRAPHY EXPORTS ============================================================================================
    @Override
    public ExportResult getAuthorFWBBibliography(
        Context context,
        String authorUUID,
        String authorFGS,
        Map<String, String> filters
    ) throws AuthorNotFoundException, SearchServiceException, CrosswalkException {
        Item author = findAuthor(context, authorUUID, authorFGS);
        Iterator<Item> publications = findFWBPublications(context, author.getID().toString(), filters);
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(FWB_CROSSWALK_KEY);
        itemCrosswalk.addTransformerParameter("highlightText", author.getName());
        itemCrosswalk.addTransformerParameter("documentTitle", FWB_DOCUMENT_TITLE.formatted(author.getName()));
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }

    @Override
    public ExportResult getAuthorFNRSBibliography(
        Context context,
        String authorUUID,
        String authorFGS,
        Map<String, String> filters
    ) throws AuthorNotFoundException, SearchServiceException, CrosswalkException {
        Item author = findAuthor(context, authorUUID, authorFGS);
        Iterator<Item> publications = findFNRSPublications(context, author.getID().toString(), filters);
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(FNRS_CROSSWALK_KEY);
        itemCrosswalk.addTransformerParameter("highlightText", author.getName());
        itemCrosswalk.addTransformerParameter("documentTitle", FNRS_DOCUMENT_TITLE.formatted(author.getName()));
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }

    // CUSTOM EXPORT ===================================================================================================
    @Override
    public ExportResult getExportResult(
        Context context,
        String crosswalkName,
        List<Pair<String, String>> queryParts,
        QueryOperator operator,
        Map<String, String> filters,
        SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws SearchServiceException, CrosswalkException {
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(crosswalkName);
        if (queryParts.isEmpty()) {
            throw new IllegalArgumentException("Query cannot be empty");
        }
        String solrQuery = buildSearchQuery(queryParts, operator);
        Iterator<Item> publications = publicationService
            .findPublications(context, solrQuery, filters, sort.toString(), direction)
            .map(Publication::getItem)
            .iterator();
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }
    private String buildSearchQuery(List<Pair<String, String>> parts, QueryOperator operator) {
        String joiner = " %s ".formatted(operator.toString().toUpperCase());
        return parts.stream()
            .map(p -> "%s:\"%s\"".formatted(p.getLeft(), ClientUtils.escapeQueryChars(p.getRight())))
            .collect(Collectors.joining(joiner));
    }

    // 'FIND BY' EXPORT ================================================================================================

    /**
     * Allow to find publications based on publications authors.
     * Each provided identifiers must be a valid identifier type (at this time we allow "uuid", "fgs", "name")
     */
    @Override
    public ExportResult findByAuthor(
        Context context,
        List<Pair<String, String>> authorIdentifiers,
        String crosswalk,
        Map<String, String> filters,
        SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws CrosswalkException, SearchServiceException {
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(crosswalk);
        Iterator<Item> publications = publicationService
            .findByAuthors(context, authorIdentifiers, filters, sort, direction)
            .map(Publication::getItem)
            .iterator();
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }

    /** Allow to export publication related to given affiliation names */
    @Override
    public ExportResult findByAffiliationByName(
        Context context,
        List<String> affiliationNames,
        String crosswalk,
        Map<String, String> filters,
        SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws CrosswalkException, AffiliationNotFoundException, SearchServiceException {
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(crosswalk);
        Iterator<Item> publications = publicationService
            .findByAffiliationNames(context, affiliationNames, filters, sort, direction)
            .map(Publication::getItem)
            .iterator();
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }

    /** Allow to export publication related to given affiliation uuids */
    @Override
    public ExportResult findByAffiliationByUUID(
        Context context,
        List<String> affiliationUUIDs,
        boolean includeDescendant,
        String crosswalk,
        Map<String, String> filters,
        SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws CrosswalkException, AffiliationNotFoundException, SearchServiceException {
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(crosswalk);
        Iterator<Item> publications = publicationService
            .findByAffiliationUUIDs(context, affiliationUUIDs, includeDescendant, filters, sort, direction)
            .map(Publication::getItem)
            .iterator();
        return new TempFileExportResult(context, itemCrosswalk, publications);
    }

    @Override
    public ExportResult findByFunding(
        Context context,
        String organization,
        String program,
        String crosswalk,
        Map<String, String> filters,
        SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws CrosswalkException, CrosswalkNotFoundException, SearchServiceException, ParseException {
        ItemExportCrosswalk itemCrosswalk = findItemExportCrosswalk(crosswalk);
        Iterator<Item> publications = publicationService
            .findByFunding(context, organization, program, filters, sort, direction)
            .map(Publication::getItem)
            .iterator();
        return new TempFileExportResult(context, itemCrosswalk, publications);
    };

    // PRIVATE METHODS -------------------------------------------------------------------------------------------------

    /**
     * Find all FWB valid publications for a given author.
     * @param context The current DSpace context.
     * @param authorId The id of the author.
     * @param baseFilters the map of filters to use to limit publication result.
     * @return An iterator of all FWB valid publication items of the given author.
     * @throws SearchServiceException if any exception occurred during search
     */
    private Iterator<Item> findFWBPublications(
        Context context,
        String authorId,
        Map<String, String> baseFilters
    ) throws SearchServiceException {
        String query = String.format("isAuthorOfPublication:\"%s\"", authorId);
        Map<String, String> mainFilters = Map.of(
            "entityType", Publication.ENTITY_TYPE,
            "fwbExportable", "true"
        );
        Map<String, String> combinedFilters = Stream
            .concat(baseFilters.entrySet().stream(), mainFilters.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (entry1, entry2) -> entry2));
        return publicationService.findPublications(context, query, combinedFilters, null, null)
            .map(Publication::getItem)
            .iterator();
    }

    /**
     * Find all FNRS valid publications of a given author.
     * The publications have to be valid (FNRSValid) and also the author has to be a valid FNRS author (role validation)
     * 
     * @param context The current DSpace context.
     * @param authorId The UUID of the author to get publications of.
     * @param baseFilters the map of filters to use to limit publication result.
     * @return An iterator of all FNRS valid publications for the given author.
     * @throws SearchServiceException if any exception occurred during search
     */
    private Iterator<Item> findFNRSPublications(
        Context context,
        String authorId,
        Map<String, String> baseFilters
    ) throws SearchServiceException {
        // First find unfiltered publications.
        String query = String.format("isAuthorOfPublication:\"%s\"", authorId);
        Map<String, String> mainFilters = Map.of(
            "entityType", Publication.ENTITY_TYPE,
            "fnrsValid", "true"
        );
        Map<String, String> combinedFilters = Stream
            .concat(baseFilters.entrySet().stream(), mainFilters.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (entry1, entry2) -> entry2));
        Stream<Publication> publications = publicationService.findPublications(
            context,
            query,
            combinedFilters,
            null,
            null
        );
        // Now, we need to filter each publication depending on the corresponding author (because some publication are
        // valid for in regard to generic FNRS rules, but not for this specific author (author role, ...))
        return publications.filter(publication -> {
            try {
                return FNRSExportUtils.isFNRSValid(authorId, publication);
            } catch (Exception e) {
                logger.warn("Skipping publication {} due to FNRS validation error", publication.getID(), e);
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
        if (!(crosswalk instanceof ItemExportCrosswalk)) {
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
     * @throws AuthorNotFoundException if no author item could be found using the provided identifiers.
     */
    private Item findAuthor(Context context, String authorUUID, String authorFGS) throws AuthorNotFoundException {
        Item author = uclouvainProfileService.findByIdentifiers(context, authorUUID, authorFGS, null);
        if (author == null) {
            throw new AuthorNotFoundException("Could not find any matching author for given identifiers.");
        }
        return author;
    }
}
