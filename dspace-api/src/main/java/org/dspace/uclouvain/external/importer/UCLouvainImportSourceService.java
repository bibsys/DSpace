/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.importer;

import java.util.List;

import org.dspace.content.dto.MetadataValueDTO;

public interface UCLouvainImportSourceService {
    public List<MetadataValueDTO> getMetadataList(String query);

    /**
     * Retrieve the total result count for a given query.
     * This should be implemented if pagination is needed.
     * 
     * @param query The query used to find records.
     * @return The number of records matching the given query.
     */
    public default int getResultCount(String query) {
        // ! See what to do here since we dont want to create a specific count method in importService...
        // Used by the ExternalSourceRestRepository when importing items from external source in the frontend.
        // TODO: Return results count.
        return 0;
    }
}
