/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.FWBValidation;

/**
 * Main service to check for eligibility and compliance of an item based on the FWB OpenAccess decree rules.
 * 
 * IMPORTANT: The `isFWBEligible()` method should be executed first to check for the eligibility
 * of the item before executing `isFWBCompliant()`.
 */
public interface UCLouvainFWBValidationService {
    public boolean isFWBEligible(Context context, Item item);
    public boolean isFWBCompliantAsBoolean(Context context, Item item);
    public FWBValidation isFWBCompliant(Context context, Item item);
}