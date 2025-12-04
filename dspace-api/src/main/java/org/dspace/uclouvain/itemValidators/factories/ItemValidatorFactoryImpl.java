/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemValidators.factories;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.uclouvain.itemValidators.ItemValidator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory to retrieve a specific ItemValidator for a given item.
 */
public class ItemValidatorFactoryImpl implements ItemValidatorFactory {

    private Map<String, ItemValidator> validatorsMap = new HashMap<>();

    @Autowired
    protected ItemService itemService;

    // PUBLIC METHODS

    @Override
    public ItemValidator getValidator(Item item) {
        return getValidator(itemService.getEntityType(item));
    }

    @Override
    public ItemValidator getValidator(String entityType) {
        return StringUtils.isNotBlank(entityType)
            ? validatorsMap.get(entityType)
            : null;
    }

    // SETTERS AND GETTERS

    public Map<String, ItemValidator> getValidatorsMap() {
        return validatorsMap;
    }

    public void setValidatorsMap(Map<String, ItemValidator> validatorsMap) {
        this.validatorsMap = validatorsMap;
    }
}
