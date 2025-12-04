/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemValidators;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.itemValidators.exceptions.ItemValidationException;

/**
 * An item validator is a class that can be used to process some validation on an item before creating/updating it.
 * This can be especially useful when checking for duplicates for example.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public interface ItemValidator {
    public void validate(Context context, Item item) throws ItemValidationException;
}
