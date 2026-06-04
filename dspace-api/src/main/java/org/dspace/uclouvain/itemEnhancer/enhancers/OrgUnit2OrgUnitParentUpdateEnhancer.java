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

/**
 * Enhancer to keep the parent of a OrgUnit up to date with the original OrgUnit.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class OrgUnit2OrgUnitParentUpdateEnhancer extends MetadataUpdateEnhancer {
    private static final Map<String, String> fieldMapping = Map.of(
        OrgUnit.TITLE_FIELD, OrgUnit.PARENT_ORGANIZATION
    );
    private static final MetadataField linkMD = new MetadataField(OrgUnit.PARENT_ORGANIZATION);

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
