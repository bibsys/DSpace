package org.dspace.uclouvain.authority.client;

import org.dspace.uclouvain.external.dilbert.model.DialPerson;

public interface UCLouvainAuthorityClient {
    public DialPerson[] getSuggestionByTermWithFilter(String query, String filterKey);
}
