/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.degreeMappers.factory;

import org.dspace.uclouvain.degreeMappers.DegreeMappersService;
import org.springframework.beans.factory.annotation.Autowired;

public class DegreeMappersServiceFactoryImpl extends DegreeMappersServiceFactory {

    @Autowired
    DegreeMappersService degreeMappersService;

    @Override
    public DegreeMappersService getDegreeMappersService() {
        return this.degreeMappersService;
    }
}
