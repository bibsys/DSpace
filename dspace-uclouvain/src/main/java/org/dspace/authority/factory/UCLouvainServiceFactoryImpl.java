package org.dspace.authority.factory;

import org.dspace.authority.client.UCLouvainAuthorAuthorityClient;
import org.dspace.authority.configuration.UCLouvainAuthorAuthorityAPIConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UCL's service factory to retrieve an instance of UCLouvainClient & UCLouvainAuthorityConfig
 * 
 * @author Laurent Dubois (laurent.dubois@uclouvain.be)
 * @co-author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
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