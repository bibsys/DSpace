/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.factories;

import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of UCLouvain resource policy service factory.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class UCLouvainResourcePolicyServiceFactoryImpl extends UCLouvainResourcePolicyServiceFactory {

    @Autowired
    private UCLouvainResourcePolicyService uclouvainResourcePolicyService;

    @Override
    public UCLouvainResourcePolicyService getResourcePolicyService() {
        return uclouvainResourcePolicyService;
    }
}
