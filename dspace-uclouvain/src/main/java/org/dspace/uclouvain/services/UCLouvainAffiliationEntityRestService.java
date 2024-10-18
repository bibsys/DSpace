package org.dspace.uclouvain.services;

import java.util.List;

import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.AffiliationEntityRestModel;

public interface UCLouvainAffiliationEntityRestService {
    public List<AffiliationEntityRestModel> getAffiliationsEntities(Context context);
}