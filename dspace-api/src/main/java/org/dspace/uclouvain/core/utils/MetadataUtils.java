/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.utils;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;

/**
 * Set of util methods for `MetadataValues`
 */
public class MetadataUtils {


    private MetadataUtils() {
        throw new UnsupportedOperationException("Utility class: Can't be instanced");
    }

    /**
     * Recovers the value for a specific field of a `MetadataValue` list.
     *
     * @param metadataValues The list of `MetadataValue` to search from.
     * @param fieldId The field to recover the value of.
     * @return Value for the given metadata field (empty string if not found).
     */
    public static String getMetadataFieldValueByFieldId(
            List<MetadataValue> metadataValues, String fieldId, String defaultValue) {
        for (MetadataValue metadataValue : metadataValues) {
            if (metadataValue.getMetadataField().toString().equals(fieldId)) {
                return metadataValue.getValue();
            }
        }
        return defaultValue;
    }

    public static String getMetadataFieldValueByFieldId(List<MetadataValue> metadataValues, String fieldId) {
        return getMetadataFieldValueByFieldId(metadataValues, fieldId, "");
    }

    /**
     * Recovers a list of all the values for a specific field of a 'MetadataValue' list.
     *
     * @param metadataValues The list of `MetadataValue` to search from.
     * @param fieldId The field to recover values of.
     * @return List of values for the given metadata field (empty list if no found).
     */
    public static List<String> getAllMetadataFieldValuesByFieldId(List<MetadataValue> metadataValues, String fieldId) {
        List<String> values = new ArrayList<>();
        for (MetadataValue metadataValue : metadataValues) {
            if (metadataValue.getMetadataField().toString().equals(fieldId)) {
                values.add(metadataValue.getValue());
            }
        }
        return values;
    }

    /**
     * This method is used to extract the real type about a DSpace item.
     *
     * @param inputType DSpace item type on "xx::xx::xx" format
     * @return The last element of the string split by "::"
     */
    public static String extractItemType(String inputType) {
        String[] parts = inputType.split("::");
        return parts[parts.length - 1];
    }

    /** 
    * Converts a list of metadataValues into a HashMap for easier data access.
    * 
    * @param metadataValues The list of `MetadataValue` to insert into the HashMap.
    * @return The HashMap with all the values (key=fieldName, value= List of strings values).
    */
    public static HashMap<String, List<String>> getValuesHashMap(List<MetadataValue> metadataValues) {
        HashMap<String, List<String>> hashMap = new HashMap<>();
        for (MetadataValue metadataValue : metadataValues) {
            String metadataField = metadataValue.getMetadataField().toString();
            List<String> currentValueForFieldId = hashMap.getOrDefault(metadataField, new ArrayList<>());
            currentValueForFieldId.add(metadataValue.getValue());
            // If the field id is not in the hashMap yet
            hashMap.putIfAbsent(metadataField, currentValueForFieldId);
        }
        return hashMap;
    }

    /**
     * Set a secured metadata for a specific metadata field.
     * This method overrides the existing values and sets a unique secured one.
     * Be aware of committing to save changes.
     * 
     * @param context The current DSpace context.
     * @param item The item to perform the change on.
     * @param schema The schema of the field to set.
     * @param element The element of the field to set.
     * @param qualifier The qualifier of the field to set.
     * @param language The language of the field to set.
     * @param value The value of the field to set.
     * @param authority The authority of the field to set.
     * @param confidence The confidence of the field to set.
     * @param security The security option of the field to set (0 to 2).
     * @throws SQLException
     */
    public static void setSecuredMetadataSingleValue(
        Context context, Item item, String schema, String element, String qualifier,
        String language, String value, String authority, int confidence, Integer security
    ) throws SQLException {
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        // Clear all present metadata and add a single secured metadata.
        List<MetadataValue> presentMetadata = itemService.getMetadata(
            item, schema, element, qualifier, language
        );
        // Clear all those metadata
        itemService.removeMetadataValues(context, item, presentMetadata);
        // add a single secured metadata value.
        itemService.addSecuredMetadata(
            context, item,
            schema, element, qualifier,
            language, value, authority, confidence, security
        );
    }

    /**
     * Predicate function that could be used to {@code .fitler()} a {@code Stream<MetadataValue>}
     */
    public static Predicate<MetadataValue> filterValidValues() {
        return mv -> StringUtils.isNotBlank(mv.getValue()) && !mv.getValue().equals(PLACEHOLDER_PARENT_METADATA_VALUE);
    }
}
