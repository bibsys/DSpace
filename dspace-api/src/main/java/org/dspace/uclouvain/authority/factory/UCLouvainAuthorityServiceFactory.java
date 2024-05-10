/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.authority.client.UCLouvainAuthorityClient;
import org.dspace.uclouvain.authority.configuration.UCLouvainAuthorityAPIConfiguration;

/**
 * Main interface for UCLouvainAuthorityServiceFactory
 * 
 * @author Laurent Dubois (laurent.dubois@uclouvain.be)
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public interface UCLouvainAuthorityServiceFactory {

    UCLouvainAuthorityClient getUCLouvainAuthorityClient();

    UCLouvainAuthorityAPIConfiguration getUCLouvainAuthorityConfiguration();

    static UCLouvainAuthorityServiceFactory getInstance() {
        return DSpaceServicesFactory
                .getInstance()
                .getServiceManager()
                .getServiceByName("uclouvainAuthorityServiceFactory", UCLouvainAuthorityServiceFactory.class);
    }
}