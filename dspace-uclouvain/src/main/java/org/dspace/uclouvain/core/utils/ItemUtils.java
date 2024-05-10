package org.dspace.uclouvain.core.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.services.factory.DSpaceServicesFactory;

/** 
* Set of util methods for an `Item` object.
*/
public class ItemUtils {
    /** 
    * This method is used to extract the files's bit stream from an item.
    * 
    * @param DSpaceItem: The item to extract files from.
    * @return The list of bit streams for the given item.
    */
    public static List<Bitstream> extractItemFiles(Item DSpaceItem) {
        // Configuration which indicates the bundles to use. 
        List<String> acceptedBundles = Arrays.asList(
            DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("uclouvain.resource_policy.accepted_bundles")
        );
        List<Bitstream> bitstreams = new ArrayList<Bitstream>();
        for (Bundle bundle: DSpaceItem.getBundles()) {
            if (acceptedBundles.contains(bundle.getName())) {
                bitstreams.addAll(bundle.getBitstreams());
            }
        }
        return bitstreams;
    }
}
