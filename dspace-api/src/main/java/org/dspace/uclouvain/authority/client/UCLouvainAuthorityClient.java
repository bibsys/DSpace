/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority.client;

import org.dspace.uclouvain.external.dilbert.model.DialPerson;

public interface UCLouvainAuthorityClient {
    DialPerson[] getSuggestionByTermWithFilter(String query, String filterKey);
}
