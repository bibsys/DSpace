/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.factories;

import org.dspace.uclouvain.citations.UCLouvainCitationsService;
import org.dspace.uclouvain.content.cleanMetadata.CleanMetadataService;
import org.dspace.uclouvain.content.service.CommentService;
import org.dspace.uclouvain.core.mails.metadataParser.MailMetadataParserService;
import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.dspace.uclouvain.itemEnhancer.poller.UCLouvainItemEnhancerPoller;
import org.dspace.uclouvain.profileIngester.services.IDMPersonValidityService;
import org.dspace.uclouvain.services.DirectLinkService;
import org.dspace.uclouvain.services.JournalService;
import org.dspace.uclouvain.services.OrgUnitService;
import org.dspace.uclouvain.services.PublicationService;
import org.dspace.uclouvain.services.UCLouvainAffiliationEntityRestService;
import org.dspace.uclouvain.services.UCLouvainEntityService;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of UCLouvain service factory.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
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
    private UCLouvainItemEnhancerPoller uclouvainItemEnhancerPoller;
    @Autowired
    private CommentService commentService;
    @Autowired
    private DirectLinkService uclouvainDirectLinkService;
    @Autowired
    private MailMetadataParserService mailMetadataParserService;
    @Autowired
    private UCLouvainAffiliationEntityRestService uclouvainAffiliationEntityRestService;
    @Autowired
    private UCLouvainCitationsService uclouvainCitationsService;
    @Autowired
    private JournalService journalService;
    @Autowired
    private OrgUnitService orgUnitService;
    @Autowired
    private PublicationService publicationService;
    @Autowired
    private IDMPersonValidityService idmPersonValidityService;
    @Autowired
    private CleanMetadataService cleanMetadataService;

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
    public UCLouvainItemEnhancerPoller getItemEnhancerUpdatePoller() {
        return uclouvainItemEnhancerPoller;
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
    @Override
    public UCLouvainAffiliationEntityRestService getAffiliationEntityRestService() {
        return uclouvainAffiliationEntityRestService;
    }
    @Override
    public UCLouvainCitationsService getCitationsService() {
        return uclouvainCitationsService;
    }
    @Override
    public JournalService getJournalService() {
        return journalService;
    }
    @Override
    public OrgUnitService getOrgUnitService() {
        return orgUnitService;
    }
    @Override
    public PublicationService getPublicationService() {
        return publicationService;
    }
    @Override
    public IDMPersonValidityService getIDMPersonValidityService() {
        return idmPersonValidityService;
    }
    @Override
    public CleanMetadataService getCleanMetadataService() {
        return cleanMetadataService;
    }

}
