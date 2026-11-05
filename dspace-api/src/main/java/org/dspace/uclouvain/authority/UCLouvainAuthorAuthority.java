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

import org.apache.commons.lang3.StringUtils;
import org.dspace.uclouvain.core.model.Orcid;
import org.dspace.uclouvain.external.patority.model.PatronModel;

/**
 * This is an implementation for the UCLouvain's ChoiceAuthority which manages the addition of authors (students).
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainAuthorAuthority extends AbstractUCLouvainAuthority {

    /** IDM grids of the students. */
    private static final int[] TARGET_IDM_GRIDS = {48, 400};
    private static final String TARGET_EMAIL_DOMAIN = "@student.uclouvain.be";

    @Override
    protected int[] getIdmGrids() {
        return TARGET_IDM_GRIDS.clone();
    }

    /**
     * Generate a list of extra information that will be displayed under the main information
     * Also used to populate the fields when the user selects a choice in the list
     *
     * @param patron One patron (result from a search) that will be used to generate extra data
     * @return A map containing the extras
     */
    @Override
    Map<String, String> generateExtras(PatronModel patron) {
        Map<String, String> extras = new HashMap<>();

        String fgs = StringUtils.defaultString(patron.getFgsIdentifier());
        String noma = StringUtils.defaultString(patron.getNomaIdentifier());
        Orcid orcid = new Orcid(StringUtils.defaultString(patron.getOrcidIdentifier()).trim());

        // Clear the previous selected value for the degree code when there is a new search
        extras.put("data-masterthesis_degree_code", "");

        // String with "data-" will be used to fill other form fields
        extras.put("data-authors_institution_name", StringUtils.defaultString(patron.getInstitution()));
        extras.put("data-authors_email_official", getEmailWithDomain(patron, TARGET_EMAIL_DOMAIN));

        // The values are always set (empty string by default) to override any previously selected value
        extras.put("data-authors_identifier_fgs", fgs);
        extras.put("data-authors_identifier_noma", noma);
        if (!noma.isEmpty()) {
            extras.put("authors-identifier-noma", noma);
        }
        if (!orcid.getOrcid().isEmpty()) {
            // For future usage, metadata reference might need to be changed
            extras.put("data-person_identifier_orcid", orcid.getID());
        }
        return extras;
    }
}
