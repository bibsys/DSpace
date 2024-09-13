package org.dspace.uclouvain.factories;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.services.UCLouvainEntityService;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.dspace.uclouvain.services.UCLouvainAffiliationEntityRestService;

/**
 * Abstract factory to get services for the UCLouvain package.
 * use UCLouvainServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public abstract  class UCLouvainServiceFactory {

    public abstract UCLouvainResourcePolicyService getResourcePolicyService();
    public abstract UCLouvainEntityService getEntityService();
    public abstract UCLouvainAffiliationEntityRestService getAffiliationEntityRestService();

    public static UCLouvainServiceFactory getInstance() {
        return DSpaceServicesFactory
                .getInstance()
                .getServiceManager()
                .getServiceByName("uclouvainServiceFactory", UCLouvainServiceFactory.class);
    }
}
