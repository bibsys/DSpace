/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.enhancers;

import java.util.Map;

import org.dspace.profile.ResearcherProfile;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.model.OrgUnit;

/**
 * Enhancer to keep the department of a person profile up to date with its corresponding OrgUnit.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class OrgUnit2PersonDepartmentUpdateEnhancer extends MetadataUpdateEnhancer {

    private static final Map<String, String> fieldMapping = Map.of(
        OrgUnit.TITLE_FIELD, ResearcherProfile.DEPARTMENT_FIELD
    );
    private static final MetadataField linkMD = new MetadataField(ResearcherProfile.DEPARTMENT_FIELD);

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
