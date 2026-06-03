/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;
import org.dspace.uclouvain.itemEnhancer.model.ItemToEnhance;

public interface UCLouvainItemEnhancerDAO {
    public void addOrUpdateItemToUpdate(Context context, UUID uuid, String entityType) throws SQLException;
    public List<ItemToEnhance> getItemsToEnhance(Context context, Integer limit) throws SQLException;
    public Integer countItemsToEnhance(Context context) throws SQLException;
    public Integer cleanTableEntriesForItem(Context context, UUID uuid) throws SQLException;
    public Integer cleanTableEntries(Context context, Date startDate, Date endDate) throws SQLException;
    public List<Pair<Item, Integer>> getAuthorityLinkedItem(
        Context context, MetadataField metadataField, String authority
    ) throws SQLException;
}