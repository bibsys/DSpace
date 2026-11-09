/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.patority.client;

import org.dspace.uclouvain.external.patority.model.PatoritySearchQuery;
import org.dspace.uclouvain.external.patority.model.PatoritySearchResult;

/**
 * Client to interact with the Patority API.
 */
public interface PatorityClient {

    /**
     * Search patrons in Patority.
     *
     * @param query the search criteria (see {@link PatoritySearchQuery#builder(String)})
     * @return the requested page of patrons with the total number of matches, or an empty result if the
     *         search term is blank or if the call fails
     */
    public PatoritySearchResult search(PatoritySearchQuery query);
}