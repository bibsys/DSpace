/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.enhancers;

import java.util.Map;

import org.dspace.uclouvain.core.model.Journal;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.model.publication.Publication;

/**
 * Enhancer to keep the journal of a publication up to date with the original Journal object.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class Journal2PublicationJournalUpdateEnhancer extends MetadataUpdateEnhancer {

    private static final Map<String, String> fieldMapping = Map.of(
        Journal.TITLE_FIELD, Publication.JOURNAL_TITLE_FIELD,
        Journal.ISSN_FIELD, Publication.JOURNAL_ISSN_FIELD,
        Journal.EISSN_FIELD, Publication.JOURNAL_EISSN_FIELD,
        Journal.PEER_REVIEWED_FIELD, Publication.JOURNAL_PEER_REVIEWED_FIELD,
        Journal.PUBLISHER_NAME_FIELD, Publication.EDITOR_NAME_FIELD,
        Journal.PUBLISHER_LOCATION_FIELD, Publication.EDITOR_LOCATION_FIELD
    );
    private static final MetadataField linkMD = new MetadataField(Publication.JOURNAL_TITLE_FIELD);

    @Override
    protected MetadataField getLinkMD() {
        return linkMD;
    }

    @Override
    protected Map<String, String> getFieldMapping() {
        return fieldMapping;
    }

    @Override
    public String getSupportedEntityType() {
        return Journal.ENTITY_TYPE;
    }
}
