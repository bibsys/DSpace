/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority;

import java.util.Arrays;
import java.util.Map;

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
 * Base class for the UCLouvain person authorities (authors, promoters).
 *
 * Persons are searched in the Patority API, filtered on the IDM grids returned by {@link #getIdmGrids()}.
 */
public abstract class AbstractUCLouvainAuthority implements ChoiceAuthority {

    private String pluginInstanceName;

    /**
     * Entry point to retrieve a list of match for a given query (person name).
     *
     * @param query The name typed by the user, used to search for persons
     * @param start Where to start the search (index)
     * @param limit How many elements in the search result
     * @param locale The locale (unused)
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
    public String getLabel(String key, String locale) {
        return "";
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

    private PatorityClient getPatorityClient() {
        return UCLouvainAuthorityServiceFactory.getInstance().getPatorityClient();
    }

    /**
     * @return The IDM grids used to filter the persons of this authority in Patority.
     */
    protected abstract int[] getIdmGrids();

    /**
     * Generate a list of extra information that will be displayed under the main information.
     * Also used to populate the fields when the user selects a choice in the list.
     *
     * @param patron One patron (result from a search) that will be used to generate extra data.
     * @return A map containing the extras.
     */
    abstract Map<String, String> generateExtras(PatronModel patron);
}
