/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.factories;

import org.dspace.uclouvain.content.service.CommentService;
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

    @Autowired
    private UCLouvainResourcePolicyService uclouvainResourcePolicyService;
    @Autowired
    private UCLouvainEntityService uclouvainEntityService;
    @Autowired
    private UCLouvainItemEnhancerService uclouvainItemEnhancerService;
    @Autowired(required = true)
    private UCLouvainItemEnhancerUpdatePoller uclouvainItemEnhancerUpdatePoller;
    @Autowired(required = true)
    private CommentService commentService;
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
    public CommentService getCommentService() {
        return commentService;
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
