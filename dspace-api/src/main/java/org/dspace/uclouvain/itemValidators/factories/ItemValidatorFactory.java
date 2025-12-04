/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemValidators.factories;

import org.dspace.content.Item;
import org.dspace.uclouvain.itemValidators.ItemValidator;

/**
 * Factory to retrieve a {@link ItemValidator} for a given item or entityType.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public interface ItemValidatorFactory {
    /**
     * Retrieve an {@link ItemValidator} for a given item.
     * @param item The item to get a validator for.
     * @return The validator for the given item. Null if none existing for the provided item entity-type.
     */
    public ItemValidator getValidator(Item item);
    /**
     * Retrieve an {@link ItemValidator} for a given entity-type.
     * @param entityType The entity-type to get a validator for.
     * @return The validator for the given item. Null if none existing for the provided item entity-type.
     */
    public ItemValidator getValidator(String entityType);
}
