/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.factories;

import org.dspace.uclouvain.citations.UCLouvainCitationsService;
import org.dspace.uclouvain.content.service.CommentService;
import org.dspace.uclouvain.core.mails.metadataParser.MailMetadataParserService;
import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.dspace.uclouvain.itemEnhancer.poller.UCLouvainItemEnhancerUpdatePoller;
import org.dspace.uclouvain.services.DirectLinkService;
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
    @Autowired
    private UCLouvainItemEnhancerUpdatePoller uclouvainItemEnhancerUpdatePoller;
    @Autowired
    private CommentService commentService;
    @Autowired
    private DirectLinkService uclouvainDirectLinkService;
    @Autowired
    private MailMetadataParserService mailMetadataParserService;
    @Autowired
    private UCLouvainAffiliationEntityRestService uclouvainAffiliationEntityRestService;
    @Autowired(required = true)
    private UCLouvainFWBValidationService uclouvainFWBValidationService;
    @Autowired(required = true)
    private UCLouvainCitationsService uclouvainCitationsService;

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
    public DirectLinkService getDirectLinkService() {
        return uclouvainDirectLinkService;
    }
    @Override
    public MailMetadataParserService getMailMetadataParserService() {
        return mailMetadataParserService;
    }
    public UCLouvainAffiliationEntityRestService getAffiliationEntityRestService() {
        return uclouvainAffiliationEntityRestService;
    }
    @Override
    public UCLouvainFWBValidationService getFWBValidationService() {
        return uclouvainFWBValidationService;
    }
    @Override
    public UCLouvainCitationsService getCitationsService() {
        return uclouvainCitationsService;
    }

}
