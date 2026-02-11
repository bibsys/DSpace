/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery.indexing;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.uclouvain.core.model.OrgUnit;

/**
 * Solr indexer for `OrgUnit` special parent university keys.
 * Stores the 2 keys 'parentUniversity.acronym' and 'parentUniversity.name' in the item document.
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 */
public class SolrServicePublicationOrgUnitIndexingPlugin
    extends SolrServiceUCLouvainIndexingPlugin
    implements SolrServiceIndexPlugin {

    private static final Logger log = LogManager.getLogger(SolrServicePublicationOrgUnitIndexingPlugin.class);
    public static final String PARENT_UNIVERSITY_ACRONYM_KEY = "parentUniversity.acronym";
    public static final String PARENT_UNIVERSITY_NAME_KEY = "parentUniversity.name";

    /**
     * Add parent master entity value to allow a search on these values retrieve these documents
     * 
     * @param context The current DSpace context.
     * @param dso The DSpace Item to process.
     * @param document The Solr document to add the keys to.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void additionalIndex(Context context, IndexableObject dso, SolrInputDocument document) {
        Item item = getItem(dso);
        if (item == null || !Objects.equals(item.getItemService().getEntityType(item), OrgUnit.ENTITY_TYPE)) {
            return;
        }
        try {
            OrgUnit parentUniversity = new OrgUnit(item).getParentUniversity();
            if (parentUniversity != null) {
                String universityAcronym = parentUniversity.getAcronym();
                String universityName = parentUniversity.getTitle();
                if (!StringUtils.isBlank(universityAcronym)) {
                    document.addField(PARENT_UNIVERSITY_ACRONYM_KEY, universityAcronym);
                }
                if (!StringUtils.isBlank(universityName)) {
                    document.addField(PARENT_UNIVERSITY_NAME_KEY, universityName);
                }
            }
        } catch (Exception e) {
            log.error("Error while indexing OrgUnit data in SOLR.", e);
        }
    }
}
