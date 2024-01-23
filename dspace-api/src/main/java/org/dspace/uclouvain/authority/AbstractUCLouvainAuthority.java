package org.dspace.uclouvain.authority;

import java.util.Map;

import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.uclouvain.external.dilbert.model.DialPerson;

public abstract class AbstractUCLouvainAuthority implements ChoiceAuthority {
    
    private String pluginInstanceName;

    /** 
     * Entry point to retrieve a list of match for a given query (person name).
     * 
     * @param query: The name typed by the user, used to search for students
     * @param start: Where to start the search (index)
     * @param limit: How many elements in the search result
     * @param locale: ???
     * @return: A List of choices from the search 
     */
    @Override
    public Choices getMatches(String query, int start, int limit, String locale) {
        DialPerson[] persons = getSuggestions(query);
        Choice choices[] = new Choice[persons.length];
        for(int i = 0;i < persons.length; i++){
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
    abstract public String getLabel(String key, String locale);

    @Override
    public String getPluginInstanceName() {
        return pluginInstanceName;
    }

    @Override
    public void setPluginInstanceName(String name) {
        this.pluginInstanceName = name;
    }

    /** 
     * Retrieve the list of persons based on the query.
     * 
     * @param query: The name typed by the user, used to search for students.
     * @return: An array containing the result of the search.
     */
    abstract DialPerson[] getSuggestions(String query);

    /** 
     * Generate a list of extra information that will be displayed under the main information.
     * Also used to populate the fields when the user selects a choice in the list.
     * 
     * @param DialPerson: One dialPerson (result from a search) that will be used to generate extra data.
     * @return: An array containing the extras.
     */
    abstract Map<String, String> generateExtras(DialPerson person);
}
