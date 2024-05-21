package org.dspace.uclouvain.factories;

import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of UCLouvain resource policy service factory.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class UCLouvainResourcePolicyServiceFactoryImpl extends UCLouvainResourcePolicyServiceFactory{

    @Autowired(required = true)
    private UCLouvainResourcePolicyService uclouvainResourcePolicyService;

    @Override
    public UCLouvainResourcePolicyService getResourcePolicyService() { return uclouvainResourcePolicyService; }
}
