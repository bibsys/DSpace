/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.factories;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.dspace.uclouvain.itemEnhancer.poller.UCLouvainItemEnhancerUpdatePoller;
import org.dspace.uclouvain.services.UCLouvainEntityService;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

/**
 * Abstract factory to get services for the UCLouvain package.
 * use UCLouvainServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract  class UCLouvainServiceFactory {

    public abstract UCLouvainResourcePolicyService getResourcePolicyService();
    public abstract UCLouvainEntityService getEntityService();
    public abstract UCLouvainItemEnhancerService getItemEnhancerService();
    public abstract UCLouvainItemEnhancerUpdatePoller getItemEnhancerUpdatePoller();

    public static UCLouvainServiceFactory getInstance() {
        return DSpaceServicesFactory
                .getInstance()
                .getServiceManager()
                .getServiceByName("uclouvainServiceFactory", UCLouvainServiceFactory.class);
    }
}
