/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.journals;

import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.indexobject.IndexableItem;
import org.springframework.beans.factory.annotation.Autowired;

public class JournalServiceImpl implements JournalService {

    @Autowired
    SearchService searchService;

    /**
     * Find a journal by its issn. Might return null if the Journal is not found.
     * 
     * @param context The current DSpace context.
     * @param issn The issn of the journal to search for.
     */
    @Override
    public Journal findByIssn(Context context, String issn) throws Exception {
        DiscoverQuery dq = new DiscoverQuery();
        dq.setDSpaceObjectFilter(IndexableItem.TYPE);
        dq.setMaxResults(1);
        dq.addFilterQueries("search.entitytype:" + Journal.JOURNAL_ENTITY_TYPE);
        dq.addFilterQueries("dc.identifier.issn:" + issn);
        DiscoverResult dr = searchService.search(context, dq);
        return dr.getIndexableObjects()
            .stream()
            .map(indexableObject -> new Journal(((IndexableItem) indexableObject).getIndexedObject()))
            .findFirst()
            .orElse(null);
    }
}
