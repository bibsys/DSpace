/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions.dSpaceObjectsUpdates;

import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.subscriptions.service.DSpaceObjectUpdates;


/**
 * Class which will be used to find
 * all collection objects updated related with subscribed DSO
 *
 * @author Alba Aliu
 */
public class CollectionsUpdates implements DSpaceObjectUpdates {

    @Override
    public List<IndexableObject> findUpdates(Context context, DSpaceObject dSpaceObject, String frequency)
            throws SearchServiceException {
        DiscoverQuery discoverQuery = new DiscoverQuery();
//        discoverQuery.addFilterQueries("search.resourcetype:" + dSpaceObject.getName());
        discoverQuery.addFilterQueries("search.resourcetype:" + "Collection");
        discoverQuery.addFilterQueries("location.coll:(" + dSpaceObject.getID() + ")");
        discoverQuery.addFilterQueries("lastModified_dt:" + findLastFrequency(frequency));
        DiscoverResult discoverResult = SearchUtils.getSearchService().search(context, discoverQuery);
        return discoverResult.getIndexableObjects();
    }
}
