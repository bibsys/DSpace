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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.uclouvain.external.patority.model.PatronModel;

/**
 * This is an implementation for the UCLouvain's ChoiceAuthority which manages the addition of promoters (staff).
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class UCLouvainPromoterAuthority extends AbstractUCLouvainAuthority {

    /** IDM grids of the staff. */
    private static final int[] TARGET_IDM_GRIDS = {1, 2, 3, 4, 6, 7, 8, 9, 21, 22, 23, 24, 27, 32, 85, 91};
    private static final String TARGET_EMAIL_DOMAIN = "@uclouvain.be";

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
        String[] affiliations = patron.getAffiliations();
        if (ArrayUtils.isNotEmpty(affiliations)) {
            extras.put("institution-entity-name", String.join(", ", affiliations));
        }
        // String with "data-" will be used to fill other form fields
        extras.put("data-advisors_email", getEmailWithDomain(patron, TARGET_EMAIL_DOMAIN));
        extras.put("data-advisors_institution_name", StringUtils.defaultString(patron.getInstitution()));
        return extras;
    }
}
