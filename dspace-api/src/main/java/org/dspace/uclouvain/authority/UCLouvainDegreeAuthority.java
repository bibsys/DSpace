/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.authority;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.DegreeSearchResult;
import org.dspace.uclouvain.services.DegreeService;

/**
 * ChoiceAuthority implementation that searches for degrees from existing
 * thesis items in the Solr index via MasterThesisService.
 * <p>
 * This is ideal for the cataretro form where submitters need to browse
 * historical degrees that may no longer exist in the entity configuration.
 * <p>
 * When a degree is selected, it auto-populates the following hidden fields:
 * <ul>
 *   <li>{@code masterthesis.degree.code} - the selected degree code</li>
 *   <li>{@code masterthesis.rootdegree.code} - the root degree code</li>
 *   <li>{@code masterthesis.rootdegree.label} - the root degree label</li>
 * </ul>
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainDegreeAuthority implements ChoiceAuthority {

    private String pluginInstanceName;

    private DegreeService degreeService = UCLouvainServiceFactory.getInstance().getDegreeService();

    /**
     * Search for degrees matching the query string by querying Solr.
     * Matches against degreelabel (partial) and degreecode (exact).
     *
     * @param query  the search text entered by the user
     * @param start  index to start from
     * @param limit  maximum number of results
     * @param locale locale (unused)
     * @return Choices containing matching degrees
     */
    @Override
    public Choices getMatches(String query, int start, int limit, String locale) {
        if (StringUtils.isBlank(query)) {
            return new Choices(Choices.CF_NOTFOUND);
        }

        if (limit <= 0) {
            limit = -1; // No limit, return all results
        }

        List<DegreeSearchResult> results = degreeService.search(query, limit);
        List<Choice> choices = buildChoicesFromResults(results);
        Choice[] resultsArray = choices.toArray(new Choice[0]);

        return new Choices(resultsArray, start, results.size(), Choices.CF_AMBIGUOUS, false, -1);
    }

    /**
     * Find the best (exact) match for the given text.
     *
     * @param text   the text to match
     * @param locale locale (unused)
     * @return Choices with best match or CF_NOTFOUND
     */
    @Override
    public Choices getBestMatch(String text, String locale) {
        if (StringUtils.isBlank(text)) {
            return new Choices(Choices.CF_NOTFOUND);
        }

        List<DegreeSearchResult> results = degreeService.search(text, -1);
        if (results.isEmpty()) {
            return new Choices(Choices.CF_NOTFOUND);
        }

        List<Choice> choices = buildChoicesFromResults(results);
        Choice[] resultsArray = choices.toArray(new Choice[0]);
        return new Choices(resultsArray, 0, 1, Choices.CF_ACCEPTED, false, -1);
    }

    /**
     * Retrieve the label for a given degree key.
     * Since the key is the degree label itself, simply return it.
     *
     * @param key    the degree label
     * @param locale locale (unused)
     * @return the degree label
     */
    @Override
    public String getLabel(String key, String locale) {
        return key;
    }

    /**
     * Build Choice objects from degree search results.
     *
     * @param results the degree search results
     * @return list of choices
     */
    private List<Choice> buildChoicesFromResults(List<DegreeSearchResult> results) {
        return results.stream()
            .map(result -> new Choice(null, getChoiceLabel(result), result.degreeLabel(), generateExtras(result)))
            .toList();
    }

    private String getChoiceLabel(DegreeSearchResult result) {
        String label = result.degreeLabel();
        if (result.degreeCode() != null) {
            label = result.degreeCode() + " - " + label;
        }
        return label;
    }

    /**
     * Generate extra fields for auto-population when a degree is selected.
     *
     * @param result the degree search result
     * @return map of field keys to values for auto-population
     */
    private Map<String, String> generateExtras(DegreeSearchResult result) {
        return Map.of(
            "data-masterthesis_degree_code", StringUtils.defaultIfBlank(result.degreeCode(), ""),
            "data-masterthesis_rootdegree_code", StringUtils.defaultIfBlank(result.rootDegreeCode(), ""),
            "data-masterthesis_rootdegree_label", StringUtils.defaultIfBlank(result.rootDegreeLabel(), "")
        );
    }

    @Override
    public String getPluginInstanceName() {
        return pluginInstanceName;
    }

    @Override
    public void setPluginInstanceName(String name) {
        this.pluginInstanceName = name;
    }
}
