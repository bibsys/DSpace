/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority.factory;

import org.dspace.uclouvain.external.patority.client.PatorityClient;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * UCLouvain's authority service factory, used to retrieve the Patority client
 * 
 * @author Laurent Dubois (laurent.dubois@uclouvain.be)
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainAuthorityServiceFactoryImpl implements UCLouvainAuthorityServiceFactory {

    @Autowired
    private PatorityClient patorityClient;

    @Override
    public PatorityClient getPatorityClient() {
        return this.patorityClient;
    }
}