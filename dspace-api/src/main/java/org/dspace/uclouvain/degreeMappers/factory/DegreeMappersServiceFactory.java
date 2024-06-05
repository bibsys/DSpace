package org.dspace.uclouvain.degreeMappers.factory;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.degreeMappers.DegreeMappersService;

public abstract class DegreeMappersServiceFactory {
    public abstract DegreeMappersService getDegreeMappersService();

    // TODO: Change the location of the bean definition to "uclouvain-services.xml"
    public static DegreeMappersServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager()
                                    .getServiceByName("degreeMappersServiceFactory", DegreeMappersServiceFactory.class);
    }
}
