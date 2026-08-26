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

import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.uclouvain.authority.client.UCLouvainAuthorityClient;
import org.dspace.uclouvain.authority.factory.UCLouvainAuthorityServiceFactory;
import org.dspace.uclouvain.external.dilbert.model.DialPerson;

/**
 * This is an implementation for the UCLouvain's ChoiceAuthority which manages the addition of authors
 */
public class UCLouvainAuthorAuthority implements ChoiceAuthority {

    private String pluginInstanceName;

    private UCLouvainAuthorityClient getUCLouvainAuthorityClient() {
        return UCLouvainAuthorityServiceFactory.getInstance().getUCLouvainAuthorityClient();
    }

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
        DialPerson[] persons = getSuggestions(query);
        Choice choices[] = new Choice[persons.length];
        for (int i = 0;i < persons.length; i++) {
            DialPerson currentPerson = persons[i];
            choices[i] = new Choice(
                // Deactivating the authority key since we do not want to link our author to anything (any identifier).
                null,
                currentPerson.getFullName(),
                currentPerson.getFullName(),
                generateExtras(currentPerson)
            );
        }
        return new Choices(choices, 0, choices.length, Choices.CF_AMBIGUOUS, false, -1);
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
     * Retrieve the list of student based on the query (first and last name)
     * 
     * @param query The name typed by the user, used to search for students
     * @return An array containing the result of the search
     */
    DialPerson[] getSuggestions(String query) {
        return this.getUCLouvainAuthorityClient().getSuggestionByTermWithFilter(query, "authors");
    }

    /** 
     * Generate a list of extra information that will be displayed under the main information
     * Also used to populate the fields when the user selects a choice in the list
     * 
     * @param person One dialPerson (result from a search) that will be used to generate extra data
     * @param locale explicit localization key if available, or null
     * @return An array containing the extras
     */
    Map<String, String> generateExtras(DialPerson person) {
        Map<String, String> extras = new HashMap<>();

        String email = person.getEmail();
        String entity = person.getEntity();

        extras.put("authors-email", email);
        extras.put("data-authors_email_official", email);
        extras.put("authors-assignment", entity);

        return extras;
    }
}
