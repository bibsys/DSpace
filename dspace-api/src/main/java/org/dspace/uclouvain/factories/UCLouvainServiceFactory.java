/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.factories;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.citations.UCLouvainCitationsService;
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
import org.dspace.uclouvain.services.UCLouvainFWBValidationService;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

/**
 * Abstract factory to get services for the UCLouvain package.
 * use UCLouvainServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract  class UCLouvainServiceFactory {

    public abstract UCLouvainResourcePolicyService getResourcePolicyService();
    public abstract UCLouvainEntityService getEntityService();
    public abstract UCLouvainItemEnhancerService getItemEnhancerService();
    public abstract UCLouvainItemEnhancerPoller getItemEnhancerUpdatePoller();
    public abstract CommentService getCommentService();
    public abstract DirectLinkService getDirectLinkService();
    public abstract MailMetadataParserService getMailMetadataParserService();
    public abstract UCLouvainAffiliationEntityRestService getAffiliationEntityRestService();
    public abstract UCLouvainFWBValidationService getFWBValidationService();
    public abstract UCLouvainCitationsService getCitationsService();
    public abstract JournalService getJournalService();
    public abstract OrgUnitService getOrgUnitService();
    public abstract PublicationService getPublicationService();
    public abstract IDMPersonValidityService getIDMPersonValidityService();

    public static UCLouvainServiceFactory getInstance() {
        return DSpaceServicesFactory
                .getInstance()
                .getServiceManager()
                .getServiceByName("uclouvainServiceFactory", UCLouvainServiceFactory.class);
    }
}
