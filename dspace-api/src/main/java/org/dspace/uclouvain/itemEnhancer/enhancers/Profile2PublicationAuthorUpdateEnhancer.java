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
 * When a profile is modified, when want to update the related metadata of all the linked publications.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class Profile2PublicationAuthorUpdateEnhancer extends MetadataUpdateEnhancer {

    private static final Map<String, String> fieldMapping = Map.of(
        ResearcherProfile.NAME_FIELD, Publication.AUTHOR_NAME_FIELD,
        ResearcherProfile.OFFICIAL_EMAIL_FIELD, Publication.AUTHOR_EMAIL_FIELD,
        ResearcherProfile.ORCID_FIELD, Publication.AUTHOR_ORCID_FIELD,
        ResearcherProfile.FGS_FIELD, Publication.AUTHOR_FGS_FIELD,
        ResearcherProfile.INSTITUTION_FIELD, Publication.AUTHOR_INSTITUTION_FIELD
    );
    private static final MetadataField authorMD = new MetadataField(Publication.AUTHOR_NAME_FIELD);

    @Override
    protected MetadataField getLinkMD() {
        return authorMD;
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
