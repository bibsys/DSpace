/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.services;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

import com.drew.lang.annotations.NotNull;
import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchServiceException;
import org.dspace.uclouvain.exceptions.AffiliationNotFoundException;
import org.dspace.uclouvain.exceptions.AuthorNotFoundException;
import org.dspace.uclouvain.exceptions.CrosswalkNotFoundException;
import org.dspace.uclouvain.export.result.ExportResult;

public interface UCLouvainExportService {

    enum QueryOperator {
        and,
        or
    }

    enum SortOption {
        year,
        documentType
    }

    /**
     * Generate an export in the given style and format for a given query.
     * Stream the result to the given OutputStream.
     * @param context The current DSpace application context.
     * @param crosswalkName The name of the crosswalk to use.
     * @param queryParts query parts to use to search publication.
     *                   Each entry defined the search key and the value to filter for
     * @param operator the operator to use to join query parts (AND or OR)
     * @param filters a map of filters to use to limit the response
     * @param sort the key to use to sort result
     * @param direction the sort direction to use (ascending or descending)
     * @return An export result containing the generated export.
     * @throws SearchServiceException An error occurred when searching using the query (likely means query is invalid).
     * @throws CrosswalkException An error occurred generating the export.
     */
    ExportResult getExportResult(
        Context context,
        @NotNull String crosswalkName,
        @NotNull List<Pair<String, String>> queryParts,
        @NotNull QueryOperator operator,
        @NotNull Map<String, String> filters,
        SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws SearchServiceException, CrosswalkException;

    /**
     * Generate a PDF FWB export for a specific author using provided identifiers.
     * @param context The current DSpace application context.
     * @param authorUUID The UUID of the author to generate the bibliography of.
     * @param authorFGS The FGS of the author to generate the bibliography of.
     * @param filters a map of filters to use to limit the response
     * @return An export result containing the generated PDF.
     * @throws AuthorNotFoundException If no author could be found using provided identifiers.
     * @throws SearchServiceException If an error occurred when retrieving the author's publications.
     * @throws CrosswalkException If an error occurred when generating the FWB export.
     */
    ExportResult getAuthorFWBBibliography(
        Context context,
        String authorUUID,
        String authorFGS,
        @NotNull Map<String, String> filters
    ) throws AuthorNotFoundException, SearchServiceException, CrosswalkException;
    /**
     * Generate a PDF FNRS export for a specific author using provided identifiers.
     * @param context The current DSpace application context.
     * @param authorUUID The UUID of the author to generate the bibliography of.
     * @param authorFGS The FGS of the author to generate the bibliography of.
     * @param filters a map of filters to use to limit the response
     * @return An export result containing the generated PDF.
     * @throws AuthorNotFoundException If no author could be found using provided identifiers.
     * @throws SearchServiceException If an error occurred when retrieving the author's publications.
     * @throws CrosswalkException If an error occurred when generating the FNRS export.
     */
    ExportResult getAuthorFNRSBibliography(
        Context context,
        String authorUUID,
        String authorFGS,
        @NotNull Map<String, String> filters
    ) throws AuthorNotFoundException, SearchServiceException, CrosswalkException;

    /**
     * Generate an export of all the publications of the given author (or authors if a name was given).
     * @param context The current DSpace application context.
     * @param authorIdentifiers The list of author identifier to search for.
     *                          Each author identifier is a pair of identifier type (uuid, fgs, name, ...) and
     *                          identifier value.
     * @param crosswalk The crosswalk to use to generate the export.
     * @param filters a map of filters to use to limit the response
     * @param sort the key to use to sort result
     * @param direction the sort direction to use (ascending or descending)
     * @return An export result containing the generated export.
     * @throws CrosswalkException If an error occurred generating the export.
     * @throws CrosswalkNotFoundException If the crosswalk to generate the export is not found.
     * @throws AuthorNotFoundException If no author could be found using provided identifiers or name.
     * @throws SearchServiceException If an error occurred when retrieving the author's publications.
     */
    ExportResult findByAuthor(
        Context context,
        @NotNull List<Pair<String, String>> authorIdentifiers,
        @NotNull String crosswalk,
        @NotNull Map<String, String> filters,
        SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws CrosswalkException, SearchServiceException;


    /**
     * Generate an export of all the publications linked to given affiliation names
     * @param context The current DSpace application context.
     * @param affiliationNames The names of the affiliation.
     * @param crosswalk The crosswalk to use to generate the export.
     * @param filters a map of filters to use to limit the response
     * @param sort the key to use to sort result
     * @param direction the sort direction to use (ascending or descending)
     * @return An export result containing the generated export.
     * @throws CrosswalkException If an error occurred generating the export.
     * @throws AffiliationNotFoundException If no affiliation could be found using provided identifiers or name.
     * @throws SearchServiceException If an error occurred when retrieving the affiliation's publications.
     */
    ExportResult findByAffiliationByName(
        Context context,
        @NotNull List<String> affiliationNames,
        @NotNull String crosswalk,
        @NotNull Map<String, String> filters,
        SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws CrosswalkException, AffiliationNotFoundException, SearchServiceException;

    /**
     * Generate an export of all the publications linked to given affiliation UUIDs
     * @param context The current DSpace application context.
     * @param affiliationUUIDs The uuid identifiers of the affiliation.
     * @param includeDescendant Is the export need to include publications related to descendant entities ?
     * @param crosswalk The crosswalk to use to generate the export.
     * @param filters a map of filters to use to limit the response
     * @param sort the key to use to sort result
     * @param direction the sort direction to use (ascending or descending)
     * @return An export result containing the generated export.
     * @throws CrosswalkException If an error occurred generating the export.
     * @throws AffiliationNotFoundException If no affiliation could be found using provided identifiers or name.
     * @throws SearchServiceException If an error occurred when retrieving the affiliation's publications.
     */
    ExportResult findByAffiliationByUUID(
        Context context,
        @NotNull List<String> affiliationUUIDs,
        boolean includeDescendant,
        @NotNull String crosswalk,
        @NotNull Map<String, String> filters,
        SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws CrosswalkException, AffiliationNotFoundException, SearchServiceException;

    /**
     * Generate an export of all the publications funded by a given organization and program.
     * @param context The current DSpace application context.
     * @param organization The organization funding the publications.
     * @param program The program funding the publications.
     * @param crosswalk The crosswalk to use to generate the export.
     * @return An export result containing the generated export.
     * @throws CrosswalkException If an error occurred generating the export.
     * @throws CrosswalkNotFoundException If no crosswalk could be found with the given identifier.
     * @throws SearchServiceException If an error occurred when retrieving the publications linked to the funding.
     */
    ExportResult findByFunding(
        Context context,
        @NotNull String organization,
        String program,
        @NotNull String crosswalk,
        @NotNull Map<String, String> filters,
        SortOption sort,
        DiscoverQuery.SORT_ORDER direction
    ) throws CrosswalkException, CrosswalkNotFoundException, SearchServiceException, ParseException;
}
