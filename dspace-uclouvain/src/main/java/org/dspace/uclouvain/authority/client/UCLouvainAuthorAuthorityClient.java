package org.dspace.uclouvain.authority.client;

import org.dspace.uclouvain.dilbert.model.DialPerson;

public interface UCLouvainAuthorAuthorityClient {
    public DialPerson[] getStudentByTermWithFilter(String query);
}
