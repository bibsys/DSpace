/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.List;

import org.dspace.uclouvain.core.model.AffiliationEntityRestModel;

public interface UCLouvainAffiliationEntityRestService {
    public List<AffiliationEntityRestModel> getAffiliationsEntities();
    public void updateAffiliationEntities();
}