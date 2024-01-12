package org.dspace.authority.client;

import org.dspace.external.dilbert.model.DialPerson;

public interface UCLouvainAuthorAuthorityClient {
    public DialPerson[] getStudentByTermWithFilter(String query);
}
