/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.uclouvain.authority.factory.UCLouvainAuthorityServiceFactory;
import org.dspace.uclouvain.external.patority.client.PatorityClient;
import org.dspace.uclouvain.external.patority.model.PatoritySearchQuery;
import org.dspace.uclouvain.external.patority.model.PatoritySearchResult;
import org.dspace.uclouvain.external.patority.model.PatronModel;

/**
 * This is an implementation for the UCLouvain's ChoiceAuthority which manages the addition of authors
 */
public class UCLouvainAuthorAuthority implements ChoiceAuthority {

    private String pluginInstanceName;

    /** IDM grids of the staff. */
    private static final int[] TARGET_IDM_GRIDS = {1, 2, 3, 4, 6, 7, 8, 9, 21, 22, 23, 24, 27, 32, 85, 91};
    private static final String TARGET_EMAIL_DOMAIN = "@uclouvain.be";

    public String getLabel(String key, String locale) {
        return "";
    }

     /** 
     * Entry point to retrieve a list of match for a given query (person name).
     * 
     * @param query The name typed by the user, used to search for students
     * @param start Where to start the search (index)
     * @param limit How many elements in the search result
     * @param locale ???
     * @return A List of choices from the search
     */
    @Override
    public Choices getMatches(String query, int start, int limit, String locale) {
        if (StringUtils.isBlank(query)) {
            return new Choices(Choices.CF_NOTFOUND);
        }
        int offset = Math.max(0, start);
        PatoritySearchQuery.Builder queryBuilder = PatoritySearchQuery.builder(query)
            .idmGrids(getIdmGrids())
            .offset(offset);
        if (limit > 0) {
            // Otherwise, the default limit of the query builder is used
            queryBuilder.limit(limit);
        }
        PatoritySearchResult result = getPatorityClient().search(queryBuilder.build());
        PatronModel[] patrons = result.getResults();
        Choice[] choices = new Choice[patrons.length];
        for (int i = 0; i < patrons.length; i++) {
            PatronModel patron = patrons[i];
            choices[i] = new Choice(
                // Deactivating the authority key since we do not want to link our author to anything (any identifier).
                null,
                patron.getFullName(),
                patron.getFullName(),
                generateExtras(patron)
            );
        }
        // The total (all pages included) is used by the REST layer to compute the pagination.
        int total = Math.max(result.getTotal(), offset + choices.length);
        boolean more = offset + choices.length < total;
        return new Choices(choices, offset, total, Choices.CF_AMBIGUOUS, more, -1);
    }

    @Override
    public Choices getBestMatch(String text, String locale) {
        return new Choices(Choices.CF_NOTFOUND);
    }

    @Override
    public String getPluginInstanceName() {
        return pluginInstanceName;
    }

    @Override
    public void setPluginInstanceName(String name) {
        this.pluginInstanceName = name;
    }

    /** 
     * Generate a list of extra information that will be displayed under the main information
     * Also used to populate the fields when the user selects a choice in the list
     * 
     * @param person One dialPerson (result from a search) that will be used to generate extra data
     * @param locale explicit localization key if available, or null
     * @return An array containing the extras
     */
    Map<String, String> generateExtras(PatronModel patron) {
        Map<String, String> extras = new HashMap<>();
        String[] affiliations = patron.getAffiliations();
        if (ArrayUtils.isNotEmpty(affiliations)) {
            extras.put("authors-assignment", String.join(", ", affiliations));
        }

        extras.put("authors-email", getEmailWithDomain(patron, TARGET_EMAIL_DOMAIN));

        // String with "data-" will be used to fill other form fields
        extras.put("data-authors_email_official", getEmailWithDomain(patron, TARGET_EMAIL_DOMAIN));
        return extras;
    }

    /**
     * Retrieve the first email of a patron belonging to the given domain.
     *
     * @param patron the patron
     * @param domain the email domain, including the "@" (e.g. "@uclouvain.be")
     * @return the first matching email, or an empty string if none matches
     */
    protected String getEmailWithDomain(PatronModel patron, String domain) {
        String[] emails = patron.getEmails();
        if (emails == null) {
            return "";
        }
        return Arrays.stream(emails)
            .filter(email -> StringUtils.endsWithIgnoreCase(StringUtils.trim(email), domain))
            .map(String::trim)
            .findFirst()
            .orElse("");
    }

    protected int[] getIdmGrids() {
        return TARGET_IDM_GRIDS.clone();
    }

    private PatorityClient getPatorityClient() {
        return UCLouvainAuthorityServiceFactory.getInstance().getPatorityClient();
    }
}
