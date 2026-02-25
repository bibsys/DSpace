/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks.virtualfields;

import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualFieldCitations;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Implementation of {@link VirtualField} that generates the citation for the
 * given item or, if a relation name is provided, for all the publications
 * related to the given item. See parent class to know all possible parameters
 *
 * The specification of this class is that it use a specific data provider to get data used to generate the citation.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class VirtualFieldFNRSCitation extends VirtualFieldCitations {

    @Autowired
    @Qualifier("FnrsListItemDataProvider")
    protected ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;

    @Override
    protected DSpaceListItemDataProvider getDSpaceListItemDataProviderInstance() {
        return dSpaceListItemDataProviderObjectFactory.getObject();
    }
}
