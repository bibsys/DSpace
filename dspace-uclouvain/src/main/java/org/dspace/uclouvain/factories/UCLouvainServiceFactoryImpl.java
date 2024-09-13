/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.factories;

import org.dspace.uclouvain.services.UCLouvainEntityService;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.dspace.uclouvain.services.UCLouvainAffiliationEntityRestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of UCLouvain service factory.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class UCLouvainServiceFactoryImpl extends UCLouvainServiceFactory {

    @Autowired(required = true)
    private UCLouvainResourcePolicyService uclouvainResourcePolicyService;
    @Autowired(required = true)
    private UCLouvainEntityService uclouvainEntityService;
    @Autowired(required = true)
    private UCLouvainAffiliationEntityRestService uclouvainAffiliationEntityRestService;

    @Override
    public UCLouvainResourcePolicyService getResourcePolicyService() {
        return uclouvainResourcePolicyService;
    }
    @Override
    public UCLouvainEntityService getEntityService() {
        return uclouvainEntityService;
    }
    @Override
    public UCLouvainAffiliationEntityRestService getAffiliationEntityRestService() {
        return uclouvainAffiliationEntityRestService;
    }

}
