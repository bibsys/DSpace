/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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

    private ItemUtils() {
        throw new UnsupportedOperationException();
    }

    /**
    * This method is used to extract the attached files from an item.
    * 
    * @param item The item to extract files from.
    * @return The list of bit streams for the given item.
    */
    public static List<Bitstream> extractItemFiles(Item item) {
        // Configuration which indicates the bundles to use.
        List<String> acceptedBundles = Arrays.asList(
            DSpaceServicesFactory.getInstance().getConfigurationService()
                    .getArrayProperty("uclouvain.resource_policy.accepted_bundles")
        );
        List<Bitstream> bitstreams = new ArrayList<>();
        for (Bundle bundle: item.getBundles()) {
            if (acceptedBundles.contains(bundle.getName())) {
                bitstreams.addAll(bundle.getBitstreams());
            }
        }
        return bitstreams;
    }
}