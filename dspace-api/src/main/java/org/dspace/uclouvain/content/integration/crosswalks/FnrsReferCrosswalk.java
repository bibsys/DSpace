/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.dspace.content.integration.crosswalks.model.TemplateLine;
import org.dspace.core.Context;
import org.dspace.uclouvain.validation.fnrs.Category;
import org.dspace.uclouvain.validation.fnrs.FNRSValidator;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class FnrsReferCrosswalk extends UCLouvainReferCrosswalk {

    @Autowired
    @Qualifier("FnrsListItemDataProvider")
    protected ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;
    @Autowired
    private FNRSValidator fnrsValidator;

    @Override
    protected DSpaceListItemDataProvider getDataProviderInstance() {
        return dSpaceListItemDataProviderObjectFactory.getObject();
    }

    @Override
    protected List<String> getMetadataValuesForLine(Context context, TemplateLine line, Item item) {
        if (line.getField().equals("item.category")) {
            return List.of(getValidFnrsCategory(item));
        }
        return super.getMetadataValuesForLine(context, line, item);
    }

    private String getValidFnrsCategory(Item item) {
        return fnrsValidator.getCategories().stream()
            .filter(category -> category.isValid(item))
            .map(Category::getName)
            .findFirst()
            .orElse("fnrs.category.unknown");
    }
}
