package org.dspace.uclouvain.authority.factory;

import org.dspace.uclouvain.authority.client.UCLouvainAuthorityClient;
import org.dspace.uclouvain.authority.configuration.UCLouvainAuthorityAPIConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UCL's service factory to retrieve an instance of UCLouvainClient & UCLouvainAuthorityConfig
 * 
 * @author Laurent Dubois (laurent.dubois@uclouvain.be)
 * @co-author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainServiceFactoryImpl implements UCLouvainServiceFactory {

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