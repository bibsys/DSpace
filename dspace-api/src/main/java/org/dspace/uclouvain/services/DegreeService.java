/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.List;

/**
 * Service to search for degrees from multiple sources:
 * <ul>
 *   <li>Solr index — degrees from existing thesis items in the archive</li>
 *   <li>EntityService — configured degrees from the entity configuration file</li>
 * </ul>
 * Results are merged and deduplicated by degree label.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public interface DegreeService {

    /**
     * Search for degrees matching the given query text.
     *
     * @param query search text matched against degree labels (partial match)
     * @param limit maximum number of results
     * @return list of distinct degree search results
     */
    List<DegreeSearchResult> search(String query, int limit);
}
