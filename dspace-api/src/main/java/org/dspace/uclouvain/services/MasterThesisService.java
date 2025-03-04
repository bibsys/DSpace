/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.Iterator;

import org.apache.commons.lang3.tuple.Triple;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.SearchServiceException;

/**
 * Service for specific MasterThesis Item management
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public interface MasterThesisService {

    /**
     * Search for MasterThesis item.
     *
     * @param context the application context
     * @param criteria a list of search criteria. Each criterion is a tuple of three values:
     *                 1) search field name
     *                 2) search field value of value part (ex: "CRIM2M" or "CRIM*")
     *                 3) is exact term search
     * @return a list of corresponding Item
     * @throws SearchServiceException for any search exception (solr, database, ...)
     */
    Iterator<Item> search(Context context, Triple<String, Object, Boolean>... criteria) throws SearchServiceException;

}
