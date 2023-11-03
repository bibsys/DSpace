/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority.factory;

import org.dspace.uclouvain.authority.client.UCLouvainAuthorAuthorityClient;
import org.dspace.uclouvain.authority.configuration.UCLouvainAuthorAuthorityAPIConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UCL's service factory to retrieve an instance of UCLouvainClient & UCLouvainAuthorityConfig
 * 
 * @author Laurent Dubois (laurent.dubois@uclouvain.be)
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainServiceFactoryImpl implements UCLouvainServiceFactory {

    @Autowired
    private UCLouvainAuthorAuthorityClient uclouvainAuthorAuthorityClient;
    @Autowired
    private UCLouvainAuthorAuthorityAPIConfiguration uclouvainAuthorAuthorityAPIConfiguration;

    @Override
    public UCLouvainAuthorAuthorityClient getUCLouvainAuthorAuthorityClient() {
        return this.uclouvainAuthorAuthorityClient;
    }

    @Override
    public UCLouvainAuthorAuthorityAPIConfiguration getUCLouvainAuthorAuthorityConfiguration() {
        return this.uclouvainAuthorAuthorityAPIConfiguration;
    }
}