package org.dspace.authority.factory;

import org.dspace.authority.client.UCLouvainAuthorAuthorityClient;
import org.dspace.authority.configuration.UCLouvainAuthorAuthorityAPIConfiguration;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Main interface for UCLouvainServiceFactory
 * 
 * @author Laurent Dubois (laurent.dubois@uclouvain.be)
 * @co-author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public interface UCLouvainServiceFactory {

    UCLouvainAuthorAuthorityClient getUCLouvainAuthorAuthorityClient();

    UCLouvainAuthorAuthorityAPIConfiguration getUCLouvainAuthorAuthorityConfiguration();

    static UCLouvainServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
                "uclouvainAuthorAuthorityServiceFactory", UCLouvainServiceFactory.class);
    }
}