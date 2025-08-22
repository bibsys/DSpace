/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;

public interface UCLouvainProfileService {
    public Item findById(Context context, String fgs) throws Exception;
    public List<Item> findLinkedPublications(Context context, Item profile) throws Exception;
    public Item createEmptyProfile(Context context, String fgs) throws Exception;
}
