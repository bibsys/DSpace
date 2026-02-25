/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks.virtualfields;

import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.core.Context;
import org.dspace.uclouvain.validation.fnrs.Category;
import org.dspace.uclouvain.validation.fnrs.FNRSValidator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} that returns the FNRS category related to an {@link Item}.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class VirtualFieldFNRSCategory implements VirtualField {

    @Autowired
    private FNRSValidator fnrsValidator;

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {
        String categoryName = fnrsValidator.getCategories().stream()
            .filter(category -> category.isValid(item))
            .map(Category::getName)
            .findFirst()
            .orElse("fnrs.category.unknown");
        return new String[] { categoryName };
    }
}
