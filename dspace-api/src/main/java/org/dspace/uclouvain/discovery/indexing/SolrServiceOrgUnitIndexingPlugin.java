/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.discovery.indexing;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.OrgUnit;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;

/**
 * Solr indexer for `OrgUnit` special parent university keys.
 * Stores the 2 keys 'parentUniversity.acronym' and 'parentUniversity.name' in the item document.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class SolrServiceOrgUnitIndexingPlugin extends SolrServiceUCLouvainIndexingPlugin<OrgUnit> {

    private static final Logger log = LogManager.getLogger(SolrServiceOrgUnitIndexingPlugin.class);
    public static final String PARENT_UNIVERSITY_ACRONYM_KEY = "parentUniversity.acronym";
    public static final String PARENT_UNIVERSITY_NAME_KEY = "parentUniversity.name";

    @Override
    protected Optional<OrgUnit> buildModel(Item item) {
        try {
            return Optional.of(new OrgUnit(item));
        } catch (InvalidModelEntityTypeException e) {
            log.debug("Unable to parse item#{} as a `OrgUnit`", item.getID());
            return Optional.empty();
        }
    }

    /**
     * Add parent master entity value to allow a search on these values retrieve these documents
     *
     * @param context The current DSpace context.
     * @param orgUnit The OrgUnit to process.
     * @param document The Solr document to add the keys to.
     */
    @Override
    protected void additionalIndex(Context context, OrgUnit orgUnit, SolrInputDocument document) {
        try {
            OrgUnit parentUniversity = orgUnit.getParentUniversity();
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
