/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.enhancers;

import java.util.Map;

import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.model.OrgUnit;
import org.dspace.uclouvain.core.model.publication.Publication;

/**
 * Enhancer to link an institution of a publication to its OrgUnit.
 * If the title of the OrgUnit is modified, then the institution is updated in the publication.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class OrgUnit2PublicationInstitutionUpdateEnhancer extends MetadataUpdateEnhancer {

    private static final Map<String, String> fieldMapping = Map.of(
        OrgUnit.ACRONYM_FIELD, Publication.ENTITY_INSTITUTION_FIELD
    );
    private static final MetadataField linkMD = new MetadataField(Publication.ENTITY_INSTITUTION_FIELD);

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
        return OrgUnit.ENTITY_TYPE;
    }
}
