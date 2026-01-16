/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks;

import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class FwbReferCrosswalk extends UCLouvainReferCrosswalk {

    @Autowired
    @Qualifier("FnrsListItemDataProvider")
    protected ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;

    @Override
    protected DSpaceListItemDataProvider getDataProviderInstance() {
        return dSpaceListItemDataProviderObjectFactory.getObject();
    }
}
