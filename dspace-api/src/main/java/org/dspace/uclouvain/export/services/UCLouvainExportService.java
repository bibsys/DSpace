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
import org.dspace.uclouvain.exceptions.AuthorNotFoundException;
import org.dspace.uclouvain.exceptions.CrosswalkNotFoundException;
import org.dspace.uclouvain.export.result.ExportResult;

public interface UCLouvainExportService {
    /**
     * Generate an export in the given style and format for a given query.
     * Stream the result to the given OutputStream.
     * 
     * @param context The current DSpace application context.s
     * @param style The style of the crosswalk to use.
     * @param format The format of the crosswalk to use.
     * @param query use the query to search for a specific set of publications.
     * 
     * @throws CrosswalkNotFoundException If no crosswalk is found for given style and format.
     * @throws SearchServiceException An error occurred when searching using the query (likely means query is invalid).
     * @throws CrosswalkException An error occurred generating the export.
     */
    public ExportResult getExportResult(Context context, String style, String format, String query)
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
}
