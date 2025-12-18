/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks.csl;

import org.dspace.content.integration.crosswalks.CSLItemDataCrosswalk;
import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A simple extension of {@link CSLItemDataCrosswalk} using a specific data provider for FNRS data
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class FnrsCSLItemDataCrosswalk extends CSLItemDataCrosswalk {

    @Autowired
    @Qualifier("FnrsListItemDataProvider")
    protected ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;

    @Override
    protected DSpaceListItemDataProvider getDSpaceListItemDataProviderInstance() {
        return dSpaceListItemDataProviderObjectFactory.getObject();
    }
}
