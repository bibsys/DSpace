package org.dspace.uclouvain.services;

import java.util.List;

import org.dspace.uclouvain.core.model.AffiliationEntityRestModel;

public interface UCLouvainAffiliationEntityRestService {
    public List<AffiliationEntityRestModel> getAffiliationsEntities();
    public void updateAffiliationEntities();
}