/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.enhancers;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.content.authority.Choices.CF_UNSET;

import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.uclouvain.core.model.MetadataField;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Class to handle enhancement with update operation.
 * Extend this class to create an 'update' metadata enhancer.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public abstract class MetadataUpdateEnhancer extends MetadataEnhancer<Item> {

    @Autowired
    protected MetadataFieldService metadataFieldService;

    /**
     * Retrieve a mapping containing the fields to extract the values of and the fields to update on the target items.
     * 
     * @return A map containing the source fields as keys and the target fields as values.
     */
    protected abstract Map<String, String> getFieldMapping();
    /**
     * Get the main field of the relation. This field is used to find all related items to an updated item.
     * 
     * For example, for a Profile/Publication relation, we could have a 'dc.contributor.author' field which we will use
     * to search for related publications.
     * 
     * @return The main metadata field to search on to find related items.
     */
    protected abstract MetadataField getLinkMD();

    @Override
    public boolean enhance(Context context, Item source) throws Exception {
        if (source == null) {
            return false;
        }
        logger.info("Update of source item {}, enhancing targets...", source.getID());
        MetadataField linkMd = getLinkMD();
        boolean updateRequired = false;
        // Set of target items that were effectively modified. Used to trigger the re-indexing
        // of those items in the Solr index (see below).
        Set<Item> updatedTargets = new LinkedHashSet<>();
        // Process:
        // 1. Find linked target items.
        // 2. For each target item, enhance the metadata using the provided mapping.
        // 3. Update each modified target item so that the 'MODIFY' / 'MODIFY_METADATA' events are fired.
        try {
            // DEVNOTE: This call is supposedly optimized by Hibernate (cache logic).
            org.dspace.content.MetadataField metadata = metadataFieldService.findByElement(
                context,
                linkMd.getSchema(),
                linkMd.getElement(),
                linkMd.getQualifier()
            );
            // Retrieve all items for which the metadata field is linked to the given authority.
            // Each matching metadata fields returns the item and the place of the metadata in the item.
            // Ex: If a publication is linked on 2 'dc.contributor.author' to a profile, then the same
            // publication item will appear 2 times in the result list but with 2 different places.
            List<Pair<Item, Integer>> itemsToEnhance
                = itemEnhancerService.getAuthorityLinkedItems(context, metadata, source.getID());
            for (Pair<Item, Integer> toEnhance : itemsToEnhance) {
                Item item = toEnhance.getLeft();
                int place = toEnhance.getRight();
                try {
                    logger.info("Enhancing for update for item " + item.getID() + " at place " + place);
                    // Find the place of the target field and enhance metadata.
                    // Note: the same item can appear multiple times (one entry per linked place).
                    boolean targetUpdated = processEnhancement(context, item, source, place);
                    if (targetUpdated) {
                        updatedTargets.add(item);
                    }
                    updateRequired = targetUpdated || updateRequired;
                } catch (SQLException sqle) {
                    logger.error(
                        "Could not enhance target item %s based on source %s".formatted(
                            item.getID().toString(), source.getID().toString()
                        ), sqle
                    );
                }
            }
            // 'setMetadataInPlace' only persists the metadata values: it does NOT fire any DSpace event.
            // We need to trigger event addition by calling 'itemService.update' on each modified target item.
            // If we don't do that, nothing will be reindexed in Solr and the search results will be stale.
            for (Item updatedTarget : updatedTargets) {
                try {
                    itemService.update(context, updatedTarget);
                    logger.info(
                        "Target item {} updated to trigger the indexing consumer (Solr update)",
                        updatedTarget.getID()
                    );
                } catch (Exception updateException) {
                    // The metadata has already been persisted by 'setMetadataInPlace';
                    // failing here means the Solr index will be stale until a next full re-index or update of the item.
                    logger.error(
                        "Could not fire update event for enhanced target item %s".formatted(
                            updatedTarget.getID().toString()
                        ), updateException
                    );
                }
            }
        } catch (Exception e) {
            logger.warn("Could not retrieve authority linked items for item id :: " + source.getID().toString(), e);
        }
        return updateRequired;
    }

    /**
     * Update the each required metadata of a source item based on the configured mapping
     * and the existing metadata of the source item.
     * 
     * @param context The current DSpace application context.
     * @param target The item to update.
     * @param source The item to get values from.
     * @param place The place in which the relation is set in the target item.
     * @return A boolean indicating if an update has been made or not.
     * @throws SQLException Thrown when an update of a metadata fails.
     */
    protected boolean processEnhancement(Context context, Item target, Item source, int place) throws SQLException {
        String sourceAuthority = source.getID().toString();
        boolean updateNeeded = false;
        for (Entry<String, String> field : getFieldMapping().entrySet()) {
            String targetField = field.getValue();
            MetadataValue targetMv =
                itemService.getMetadataByMetadataStringAndPlace(target, targetField, place);
            if (targetMv == null) {
                logger.warn(
                    "Cannot update null value in target item." +
                    "The target item should have a value for the authority field (at least a placeholder)" +
                    "Source item is %s; Target item is %s; Target field is %s".formatted(
                        sourceAuthority, target.getID().toString(), targetField
                    )
                );
                continue;
            }
            String sourceValue =
                itemService.getMetadataFirstValue(source, new MetadataFieldName(field.getKey()), Item.ANY);
            // 1. If source metadata has a value:
            //      1.a. Target has an authority, however the value is not the same. => Update value of metadata.
            //      1.b. Target has no authority, however the value is the same. => Set an authority on metadata.
            //      1.c. Target has no authority, however the value is a placeholder. => Replace by source value.
            // 2. If source metadata has no value:
            //      2.a. Target has a value linked by authority. => Delete value (replace by placeholder)
            //      2.b. Target has a value not linked by authority or no value. => no update.
            String targetAuth = targetMv.getAuthority();
            String targetValue = targetMv.getValue();
            String targetLang = targetMv.getLanguage();
            int targetPlace = targetMv.getPlace();
            if (sourceValue != null) {
                if (targetAuth != null) {
                    if (!Objects.equals(targetAuth, sourceAuthority)) {
                        logger.warn(
                            "Found a metadata for an author that mention another uuid, this should not be possible");
                    } else if (!Objects.equals(targetValue, sourceValue)) {
                        // Keep the metadata value up to date.
                        itemService.setMetadataInPlace(
                            context, target, targetField,
                            targetLang, sourceValue,
                            targetAuth, targetPlace, targetMv.getConfidence()
                        );
                        updateNeeded = true;
                    }
                } else if (Objects.equals(targetValue, sourceValue)
                    || Objects.equals(targetValue, CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)) {
                    // Update the metadata authority.
                    itemService.setMetadataInPlace(
                        context, target, targetField, targetLang, sourceValue,
                        sourceAuthority, targetPlace, CF_ACCEPTED
                    );
                    updateNeeded = true;
                }
            } else if (Objects.equals(targetAuth, sourceAuthority)) {
                // Replace metadata by placeholder since it no longer exists in the source.
                itemService.setMetadataInPlace(
                    context, target, targetField,
                    targetLang, CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE,
                    null, targetPlace, CF_UNSET
                );
                updateNeeded = true;
            }
        }
        return updateNeeded;
    }

    @Override
    public String getSupportedAction() {
        return ACTION_UPDATE;
    }
}
