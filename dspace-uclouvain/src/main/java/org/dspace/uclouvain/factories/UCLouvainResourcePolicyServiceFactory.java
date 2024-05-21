package org.dspace.uclouvain.factories;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

/**
 * Abstract factory to get services for the UCLouvain resource policy package.
 * use UCLouvainResourcePolicyServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract  class UCLouvainResourcePolicyServiceFactory {

    public abstract UCLouvainResourcePolicyService getResourcePolicyService();

    public static UCLouvainResourcePolicyServiceFactory getInstance() {
        return DSpaceServicesFactory
                .getInstance()
                .getServiceManager()
                .getServiceByName("uclouvainResourcePolicyServiceFactory", UCLouvainResourcePolicyServiceFactory.class);
    }
}
