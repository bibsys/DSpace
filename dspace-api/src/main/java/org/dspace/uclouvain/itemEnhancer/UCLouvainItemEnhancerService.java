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

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.uclouvain.itemEnhancer.enhancers.MetadataEnhancer;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;

public interface UCLouvainItemEnhancerService {
    public void addItemForEnhancement(Context context, UUID uuid, String entityType);
    public List<ItemToEnhance> getItemsToEnhance(Context context);
    public List<ItemToEnhance> getItemsToEnhance(Context context, Integer limit);
    public Integer countItemsToEnhance(Context context);
    public Integer cleanForItem(Context context, UUID uuid);
    public Integer cleanForDateRange(Context context, Date startDate, Date endDate);
    public List<Pair<Item, Integer>> getAuthorityLinkedItems(Context context, MetadataField metadataField, UUID uuid);
    public List<MetadataEnhancer<Object>> getEnhancers(String entityType, String action);
    public List<MetadataEnhancer<Object>> getEnhancers(String entityEntity, List<String> actions);
}
