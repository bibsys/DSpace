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
import org.dspace.uclouvain.core.model.publication.Publication;

/**
 * Enhancer to keep the advisor of a  Publication (Dissertation) up to date with the Person object.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class Profile2PublicationAdvisorUpdateEnhancer extends MetadataUpdateEnhancer {

    private static final Map<String, String> fieldMapping = Map.of(
        ResearcherProfile.NAME_FIELD, Publication.ADVISOR_NAME_FIELD,
        ResearcherProfile.OFFICIAL_EMAIL_FIELD, Publication.ADVISOR_EMAIL_FIELD,
        ResearcherProfile.ORCID_FIELD, Publication.ADVISOR_ORCID_FIELD,
        ResearcherProfile.FGS_FIELD, Publication.ADVISOR_FGS_FIELD,
        ResearcherProfile.INSTITUTION_FIELD, Publication.ADVISOR_INSTITUTION_FIELD
    );
    private static final MetadataField linkMD = new MetadataField(Publication.ADVISOR_NAME_FIELD);

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
        return ResearcherProfile.ENTITY_TYPE;
    }
}
