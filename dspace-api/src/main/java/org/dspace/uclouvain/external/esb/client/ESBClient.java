/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.esb.client;

import org.dspace.uclouvain.external.esb.model.ESBPersonProfile;
import org.dspace.uclouvain.external.esb.model.responses.ESBPersonAffiliationResponse;
import org.dspace.uclouvain.external.esb.model.responses.ESBPersonEmailResponse;
import org.dspace.uclouvain.external.esb.model.responses.ESBPersonIDMMembershipResponse;
import org.dspace.uclouvain.external.esb.model.responses.ESBPersonMainResponse;

public interface ESBClient {
    public ESBPersonEmailResponse[] getEmailForFGS(String fgs);
    public ESBPersonEmailResponse getMainEmailForFGS(String fgs);
    public ESBPersonAffiliationResponse[] getAffiliationsForFGS(String fgs);
    public ESBPersonMainResponse getDataForFGS(String fgs);
    public ESBPersonProfile getProfileForFGS(String fgs);
    public ESBPersonIDMMembershipResponse[] getIDMMembershipsForFGS(String fgs);
}
