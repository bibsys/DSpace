/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority.factory;

import org.dspace.uclouvain.authority.client.UCLouvainAuthorityClient;
import org.dspace.uclouvain.authority.configuration.UCLouvainAuthorityAPIConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UCL's service factory to retrieve an instance of UCLouvainClient & UCLouvainAuthorityConfig
 * 
 * @author Laurent Dubois (laurent.dubois@uclouvain.be)
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainAuthorityServiceFactoryImpl implements UCLouvainAuthorityServiceFactory {

    @Autowired
    private UCLouvainAuthorityClient uclouvainAuthorityClient;

    @Autowired
    private UCLouvainAuthorityAPIConfiguration uclouvainAuthorityAPIConfiguration;

    @Override
    public UCLouvainAuthorityClient getUCLouvainAuthorityClient() {
        return this.uclouvainAuthorityClient;
    }

    @Override
    public UCLouvainAuthorityAPIConfiguration getUCLouvainAuthorityConfiguration() {
        return this.uclouvainAuthorityAPIConfiguration;
    }
}