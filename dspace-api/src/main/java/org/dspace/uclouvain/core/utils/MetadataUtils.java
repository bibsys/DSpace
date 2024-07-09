package org.dspace.uclouvain.core.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;

/** 
* Set of util methods for `MetadataValues`
*/
public class MetadataUtils {
    /** 
    * Recovers the value for a specific field of a `MetadataValue` list.
    * 
    * @param metadataValues: The list of `MetadataValue` to search from.
    * @param fieldId: The field to recover the value of.
    * @return Value for the given metadata field (empty string if not found).
    */
    public static String getMetadataFieldValueByFieldId(List<MetadataValue> metadataValues, String fieldId, String defaultValue){
        for (MetadataValue metadataValue: metadataValues) {
            if (metadataValue.getMetadataField().toString().equals(fieldId)) {
                return metadataValue.getValue();
            }
        }
        return defaultValue;
    }

    public static String getMetadataFieldValueByFieldId(List<MetadataValue> metadataValues, String fieldId){
        return getMetadataFieldValueByFieldId(metadataValues, fieldId, "");
    }

    /** 
    * Recovers a list of all the values for a specific field of a 'MetadataValue' list.
    * 
    * @param metadataValues: The list of `MetadataValue` to search from.
    * @param fieldId: The field to recover values of.
    * @return List of values for the given metadata field (empty list if no found).
    */
    public static List<String> getAllMetadataFieldValuesByFieldId(List<MetadataValue> metadataValues, String fieldId){
        List<String> values = new ArrayList<String>();
        for (MetadataValue metadataValue: metadataValues) {
            if (metadataValue.getMetadataField().toString().equals(fieldId)) {
                values.add(metadataValue.getValue());
            }
        }
        return values;
    }

    /** 
    * This method is used to extract the real type of a DSpace item.
    * 
    * @param inputType: Type of a DSpace item of form "xx::xx::xx"
    * @return The last element of the string splitted by "::"
    */
    public static String extractItemType(String inputType) {
        String[] splitted = inputType.split("::");
        return splitted[splitted.length - 1];
    }

    /** 
    * This method is used to extract the files's bit stream from an item.
    * 
    * @param DSpaceItem: The item to extract files from.
    * @return The list of bit streams for the given item.
    */
    public static List<Bitstream> extractItemFiles(Item DSpaceItem) {
        for (Bundle bundle: DSpaceItem.getBundles()) {
            if (bundle.getName().equals("ORIGINAL")) {
                return bundle.getBitstreams();
            }
        }
        return new ArrayList<Bitstream>();
    }

    /** 
    * Converts a list of metadataValues into an HashMap for easier data access.
    * 
    * @param metadataValues: The list of `MetadataValue` to insert into the HashMap.
    * @return The HashMap with all the values (key=fieldName, value= List of strings values).
    */
    public static HashMap<String, List<String>> getValuesHashMap(List<MetadataValue> metadataValues){
        HashMap<String, List<String>> hashMap = new HashMap<>();
        for (MetadataValue metadataValue: metadataValues) {
            String metadataField = metadataValue.getMetadataField().toString();
            List<String> currentValueForFieldId = hashMap.getOrDefault(metadataField, new ArrayList<String>());
            currentValueForFieldId.add(metadataValue.getValue());
            // If the field id is not in the hashMap yet
            hashMap.putIfAbsent(metadataField, currentValueForFieldId);
        }
        return hashMap;
    }
}
