package org.dspace.uclouvain.degreeMappers.factory;

import org.dspace.uclouvain.degreeMappers.DegreeMappersService;
import org.springframework.beans.factory.annotation.Autowired;

public class DegreeMappersServiceFactoryImpl extends DegreeMappersServiceFactory {
    @Autowired(required = true)
    DegreeMappersService degreeMappersService;

    @Override
    public DegreeMappersService getDegreeMappersService() {
        return this.degreeMappersService;
    }
}
