package org.dspace.uclouvain.factories;

import org.dspace.uclouvain.services.UCLouvainEntityService;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of UCLouvain service factory.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class UCLouvainServiceFactoryImpl extends UCLouvainServiceFactory {

    @Autowired(required = true)
    private UCLouvainResourcePolicyService uclouvainResourcePolicyService;
    @Autowired(required = true)
    private UCLouvainEntityService uclouvainEntityService;

    @Override
    public UCLouvainResourcePolicyService getResourcePolicyService() { return uclouvainResourcePolicyService; }
    @Override
    public UCLouvainEntityService getEntityService(){ return uclouvainEntityService; }

}
