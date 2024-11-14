/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.dao;

import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;

public interface UCLouvainItemEnhancerDAO {
    public void addOrUpdateItemToUpdate(Context context, UUID sourceUUID, UUID targetUUID) throws Exception;
    public List<ItemToEnhance> pollItemsToUpdate(Context context) throws Exception;
    public Integer cleanTableEntriesForItem(Context context, UUID uuid) throws Exception;
    public List<Item> getAuthorityLinkedItem(
        Context context, MetadataField metadataField, String authority
    ) throws Exception;
}