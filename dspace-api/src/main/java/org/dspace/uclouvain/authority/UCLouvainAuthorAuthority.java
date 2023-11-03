package org.dspace.uclouvain.authority;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.authority.Choice;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.Choices;
import org.dspace.uclouvain.authority.client.UCLouvainAuthorAuthorityClient;
import org.dspace.uclouvain.authority.configuration.UCLouvainAuthorAuthorityAPIConfiguration;
import org.dspace.uclouvain.authority.factory.UCLouvainServiceFactory;
import org.dspace.uclouvain.core.model.Orcid;
import org.dspace.uclouvain.external.dilbert.model.DialPerson;

/**
 * This is an implementation for the UCLouvain's ChoiceAuthority which manages the addition of authors
 */
public class UCLouvainAuthorAuthority implements ChoiceAuthority {

    private String pluginInstanceName;
    
    private UCLouvainAuthorAuthorityAPIConfiguration getUCLouvainAuthorAuthorityConfiguration() {
        return UCLouvainServiceFactory.getInstance().getUCLouvainAuthorAuthorityConfiguration();
    }

    private UCLouvainAuthorAuthorityClient getUCLouvainAuthorAuthorityClient() {
        return UCLouvainServiceFactory.getInstance().getUCLouvainAuthorAuthorityClient();
    }

    /** 
     * Entry point to retrieve a list of match for a given query (student name)
     * 
     * @param query: The name typed by the user, used to search for students
     * @param start: Where to start the search (index)
     * @param limit: How many elements in the search result
     * @param locale: ???
     * @return: A List of choices from the search 
     */
    @Override
    public Choices getMatches(String query, int start, int limit, String locale) {
        DialPerson[] persons = getStudentList(query);
        Choice choices[] = new Choice[persons.length];
        for(int i = 0;i < persons.length; i++){
            DialPerson currentPerson = persons[i];
            choices[i] = new Choice(
                this.getUCLouvainAuthorAuthorityConfiguration().getAuthorityName(), 
                currentPerson.getFullName(), 
                currentPerson.getFullName(), 
                generateExtras(currentPerson)
            );
        }
        return new Choices(choices, 0, choices.length, Choices.CF_AMBIGUOUS, false, -1);
    }

    // Not implemented 
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
     * Retrieve the list of student based on the query (first and last name)
     * 
     * @param query: The name typed by the user, used to search for students
     * @return: An array containing the result of the search  
     */
    public DialPerson[] getStudentList(String query) {
        return this.getUCLouvainAuthorAuthorityClient().getStudentByTermWithFilter(query);
    }

    /** 
     * Generate a list of extra information that will be displayed under the main information 
     * Also used to populate the fields when the user selects a choice in the list
     * 
     * @param DialPerson: One dialPerson (result from a search) that will be used to generate extra data
     * @return: An array containing the extras 
     */
    private Map<String, String> generateExtras(DialPerson person){
        Map<String, String> extras = new HashMap<>();

        String entity = person.getEntity();
        Orcid orcid = new Orcid(person.orcidId);
        String employeeId = person.getEmployeeId();
        String institution = person.getInstitution();

        // String with "data-" will be used to fill other form fields 
        extras.put("institution-affiliation-name", institution);
        extras.put("data-authors_institution_code", institution);
        extras.put("data-authors_email", person.getEmail());

        if(!entity.isEmpty()){
            extras.put("institution-entity-name", entity);
        }
        if(!orcid.getOrcid().isEmpty()){
            extras.put("person_identifier_orcid", orcid.getID());
        }

        // Default value for those field is an empty string because this will allow us to override the previous set value
        extras.put("data-authors_identifier_fgs", "");
        extras.put("data-authors_identifier_noma", "");
        if(institution.contains("UCL")){
            extras.put("data-authors_identifier_fgs", person.getPrimaryId());
        }
        if(!employeeId.isEmpty()){
            extras.put("data-authors_identifier_noma", employeeId);
        }

        return extras;
    }
}
