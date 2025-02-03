/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.factories;

import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.dspace.uclouvain.itemEnhancer.poller.UCLouvainItemEnhancerUpdatePoller;
import org.dspace.uclouvain.services.UCLouvainAffiliationEntityRestService;
import org.dspace.uclouvain.services.UCLouvainEntityService;
import org.dspace.uclouvain.services.UCLouvainFWBValidationService;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
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
    private UCLouvainItemEnhancerService uclouvainItemEnhancerService;
    @Autowired(required = true)
    private UCLouvainItemEnhancerUpdatePoller uclouvainItemEnhancerUpdatePoller;
    @Autowired(required = true)
    private UCLouvainAffiliationEntityRestService uclouvainAffiliationEntityRestService;
    @Autowired(required = true)
    private UCLouvainFWBValidationService uclouvainFWBValidationService;

    @Override
    public UCLouvainResourcePolicyService getResourcePolicyService() {
        return uclouvainResourcePolicyService;
    }
    @Override
    public UCLouvainEntityService getEntityService() {
        return uclouvainEntityService;
    }
    @Override
    public UCLouvainItemEnhancerService getItemEnhancerService() {
        return uclouvainItemEnhancerService;
    }
    @Override
    public UCLouvainItemEnhancerUpdatePoller getItemEnhancerUpdatePoller() {
        return uclouvainItemEnhancerUpdatePoller;
    }
    @Override
    public UCLouvainAffiliationEntityRestService getAffiliationEntityRestService() {
        return uclouvainAffiliationEntityRestService;
    }
    @Override
    public UCLouvainFWBValidationService getFWBValidationService() {
        return uclouvainFWBValidationService;
    }

}
