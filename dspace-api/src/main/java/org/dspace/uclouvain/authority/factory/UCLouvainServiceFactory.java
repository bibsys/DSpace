package org.dspace.uclouvain.authority.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.authority.client.UCLouvainAuthorityClient;
import org.dspace.uclouvain.authority.configuration.UCLouvainAuthorityAPIConfiguration;

/**
 * Main interface for UCLouvainServiceFactory
 * 
 * @author Laurent Dubois (laurent.dubois@uclouvain.be)
 * @co-author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public interface UCLouvainServiceFactory {

    UCLouvainAuthorityClient getUCLouvainAuthorityClient();

    UCLouvainAuthorityAPIConfiguration getUCLouvainAuthorityConfiguration();

    static UCLouvainServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
                "uclouvainAuthorAuthorityServiceFactory", UCLouvainServiceFactory.class);
    }
}