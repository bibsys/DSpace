/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
