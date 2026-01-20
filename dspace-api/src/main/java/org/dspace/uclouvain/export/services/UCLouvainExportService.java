/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.services;

import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;
import org.dspace.uclouvain.exceptions.AffiliationNotFoundException;
import org.dspace.uclouvain.exceptions.AuthorNotFoundException;
import org.dspace.uclouvain.exceptions.CrosswalkNotFoundException;
import org.dspace.uclouvain.export.result.ExportResult;

public interface UCLouvainExportService {
    /**
     * Generate an export in the given style and format for a given query.
     * Stream the result to the given OutputStream.
     * 
     * @param context The current DSpace application context.
     * @param crosswalkName The name of the crosswalk to use.
     * @param query use the query to search for a specific set of publications.
     * 
     * @throws CrosswalkNotFoundException If no crosswalk is found for given style and format.
     * @throws SearchServiceException An error occurred when searching using the query (likely means query is invalid).
     * @throws CrosswalkException An error occurred generating the export.
     */
    public ExportResult getExportResult(Context context, String crosswalkName, String query)
        throws CrosswalkNotFoundException, SearchServiceException, CrosswalkException;
    /**
     * Generate a PDF FWB export for a specific author using provided identifiers.
     * @param context The current DSpace application context.
     * @param authorUUID The UUID of the author to generate the bibliography of.
     * @param authorFGS The FGS of the author to generate the bibliography of.
     * @return An export result containing the generated PDF.
     * @throws AuthorNotFoundException If no author could be found using provided identifiers.
     * @throws CrosswalkNotFoundException If the crosswalk to generate the export is not found.
     * @throws SearchServiceException If an error occurred when retrieving the author's publications.
     * @throws CrosswalkException If an error occurred when generating the FWB export.
     */
    public ExportResult getAuthorFWBBibliography(Context context, String authorUUID, String authorFGS)
        throws AuthorNotFoundException, CrosswalkNotFoundException, SearchServiceException, CrosswalkException;
    /**
     * Generate a PDF FNRS export for a specific author using provided identifiers.
     * @param context The current DSpace application context.
     * @param authorUUID The UUID of the author to generate the bibliography of.
     * @param authorFGS The FGS of the author to generate the bibliography of.
     * @return An export result containing the generated PDF.
     * @throws AuthorNotFoundException If no author could be found using provided identifiers.
     * @throws CrosswalkNotFoundException If the crosswalk to generate the export is not found.
     * @throws SearchServiceException If an error occurred when retrieving the author's publications.
     * @throws CrosswalkException If an error occurred when generating the FNRS export.
     */
    public ExportResult getAuthorFNRSBibliography(Context context, String authorUUID, String authorFGS)
        throws AuthorNotFoundException, CrosswalkNotFoundException, SearchServiceException, CrosswalkException;

    /**
     * Generate an export of all the publications of the given author (or authors if a name was given).
     * @param context The current DSpace application context.
     * @param authorFGS The FGS of the author to export the publications of.
     * @param authorUUID The UUID of the author to export the publications of.
     * @param authorName The name of the author(s) to export the publication of.
     * @param crosswalk The crosswalk to use to generate the export.
     * @return An export result containing the generated export.
     * @throws CrosswalkException If an error occurred generating the export.
     * @throws CrosswalkNotFoundException If the crosswalk to generate the export is not found.
     * @throws AuthorNotFoundException If no author could be found using provided identifiers or name.
     * @throws SearchServiceException If an error occurred when retrieving the author's publications.
     */
    public ExportResult findByAuthor(
        Context context, String authorFGS, String authorUUID, String authorName, String crosswalk
    ) throws CrosswalkException, CrosswalkNotFoundException, AuthorNotFoundException, SearchServiceException;


    /**
     * Generate an export of all the publications linked to a given affiliation.
     * @param context The current DSpace application context.
     * @param affiliationUUID The main UUID of the affiliation.
     * @param affiliationName The name of the affiliation.
     * @param crosswalk The crosswalk to use to generate the export.
     * @return An export result containing the generated export.
     * @throws CrosswalkException If an error occurred generating the export.
     * @throws CrosswalkNotFoundException If no crosswalk could be found with the given identifier.
     * @throws AffiliationNotFoundException If no affiliation could be found using provided identifiers or name.
     * @throws SearchServiceException If an error occurred when retrieving the affiliation's publications.
     */
    public ExportResult findByAffiliation(
        Context context, String affiliationUUID, String affiliationName, String crosswalk
    ) throws CrosswalkException, CrosswalkNotFoundException, AffiliationNotFoundException, SearchServiceException;

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
    public ExportResult findByFunding(
        Context context, String organization, String program, String crosswalk
    ) throws CrosswalkException, CrosswalkNotFoundException, SearchServiceException;
}
