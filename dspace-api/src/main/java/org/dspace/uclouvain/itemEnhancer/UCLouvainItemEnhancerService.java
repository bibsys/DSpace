/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.itemEnhancer.enhancers.ItemEnhancerConfiguration;
import org.dspace.uclouvain.itemEnhancer.exceptions.WrongEntityTypeException;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;

public interface UCLouvainItemEnhancerService {
    public List<ItemEnhancerConfiguration> getValidConfigurationsForItem(Item item);
    public List<ItemEnhancerConfiguration> getValidConfigurationsForSourceAndTarget(Item source, Item target)
        throws WrongEntityTypeException;
    public void addRelatedItemsForEnhancement(
        Context context, Item item, List<ItemEnhancerConfiguration> validConfigurations
    );
    public List<ItemToEnhance> getItemsToEnhance(Context context);
    public List<ItemToEnhance> getItemsToEnhance(Context context, Integer limit);
    public Integer countItemsToEnhance(Context context);
    public Integer cleanForItem(Context context, UUID uuid);
    public Integer cleanForDateRange(Context context, Date startDate, Date endDate);
}
