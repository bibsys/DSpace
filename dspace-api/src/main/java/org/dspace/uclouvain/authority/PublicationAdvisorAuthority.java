/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.profile.ResearcherProfile;

/**
 * Simple authority to search for dissertation supervisor.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class PublicationAdvisorAuthority extends PublicationAuthorAuthority {
    /**
     * Generate extra information to fill some fields in the forms.
     */
    @Override
    protected Map<String, String> generateExtras(Item item) {
        Map<String, String> extras = new HashMap<>();
        fillMetadata(
            extras,
            item,
            ResearcherProfile.OFFICIAL_EMAIL_FIELD,
            "advisors_email",
            true
        );
        fillMetadata(
            extras,
            item,
            ResearcherProfile.ORCID_FIELD,
            "advisors_identifier_orcid",
            true
        );
        fillMetadata(
            extras,
            item,
            ResearcherProfile.FGS_FIELD,
            "advisors_identifier_fgs",
            true
        );
        fillMetadata(
            extras,
            item,
            ResearcherProfile.INSTITUTION_FIELD,
            "advisors_institution_code",
            true
        );
        fillMetadata(
            extras,
            item,
            ResearcherProfile.DEPARTMENT_FIELD,
            "advisors_entity_name",
            false
        );
        return extras;
    }
}
