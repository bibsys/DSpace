/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.List;
import java.util.UUID;

import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.AffiliationEntityRestModel;

public interface UCLouvainAffiliationEntityRestService {

    /**
     * This method will create the requested affiliation tree
     * @param context the dspace application context
     * @param parentUUID the optional parent UUID that should be considered as root element of the tree
     * @param depth the depth on children element from root(s) orgUnit
     * @param includeDocCount if the number of publication related to the orgUnit must be included
     * @return the list of requested orgUnit as {@link AffiliationEntityRestModel} serializable object
     */
    List<AffiliationEntityRestModel> getAffiliationsEntities(
        Context context,
        UUID parentUUID,
        int depth,
        boolean includeDocCount
    );
}