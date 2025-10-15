/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.impl;

import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.uclouvain.core.model.Journal;
import org.dspace.uclouvain.services.JournalService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link JournalService}.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class JournalServiceImpl implements JournalService {

    @Autowired
    SearchService searchService;

    @Override
    public Journal findByIssn(Context context, String issn) {
        return searchByQuery(context, "%s:\"%s\"".formatted(Journal.ISSN_FIELD, issn));
    }
    @Override
    public Journal findByEissn(Context context, String eissn) {
        return searchByQuery(context, "%s:\"%s\"".formatted(Journal.EISSN_FIELD, eissn));
    }
    @Override
    public Journal findByTitle(Context context, String journalTitle) {
        return searchByQuery(context, "%s:\"%s\"".formatted(Journal.TITLE_FIELD, journalTitle));
    }

    private Journal searchByQuery(Context context, String query) {
        DiscoverQuery dq = new DiscoverQuery();
        dq.setDSpaceObjectFilter(IndexableItem.TYPE);
        dq.setMaxResults(1);
        dq.addFilterQueries("search.entitytype:" + Journal.JOURNAL_ENTITY_TYPE);
        dq.setQuery(query);
        try {
            DiscoverResult dr = searchService.search(context, dq);
            return dr.getIndexableObjects()
                    .stream()
                    .map(indexableObject -> new Journal(((IndexableItem) indexableObject).getIndexedObject()))
                    .findFirst()
                    .orElse(null);
        } catch (SearchServiceException e) {
            throw new RuntimeException(e);
        }
    }

}
