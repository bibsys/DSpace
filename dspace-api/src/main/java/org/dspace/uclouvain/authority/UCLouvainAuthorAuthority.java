package org.dspace.uclouvain.authority;

import java.util.HashMap;
import java.util.Map;

import org.dspace.uclouvain.authority.client.UCLouvainAuthorityClient;
import org.dspace.uclouvain.authority.factory.UCLouvainServiceFactory;
import org.dspace.uclouvain.core.model.Orcid;
import org.dspace.uclouvain.external.dilbert.model.DialPerson;

/**
 * This is an implementation for the UCLouvain's ChoiceAuthority which manages the addition of authors
 */
public class UCLouvainAuthorAuthority extends AbstractUCLouvainAuthority {
    
    private UCLouvainAuthorityClient getUCLouvainAuthorityClient() {
        return UCLouvainServiceFactory.getInstance().getUCLouvainAuthorityClient();
    }

    public String getLabel(String key, String locale) {
        return "";
    }

    /** 
     * Retrieve the list of student based on the query (first and last name)
     * 
     * @param query: The name typed by the user, used to search for students
     * @return: An array containing the result of the search  
     */
    DialPerson[] getSuggestions(String query) {
        return this.getUCLouvainAuthorityClient().getSuggestionByTermWithFilter(query, "authors");
    }

    /** 
     * Generate a list of extra information that will be displayed under the main information 
     * Also used to populate the fields when the user selects a choice in the list
     * 
     * @param DialPerson: One dialPerson (result from a search) that will be used to generate extra data
     * @return: An array containing the extras 
     */
    Map<String, String> generateExtras(DialPerson person){
        Map<String, String> extras = new HashMap<>();

        String employeeId = person.getEmployeeId();
        String institution = person.getInstitution();

        Orcid orcid = new Orcid(person.orcidId);

        // Clear the previous selected value for the degree code when there is a new search
        extras.put("data-masterthesis_degree_code", "");

        // String with "data-" will be used to fill other form fields 
        extras.put("data-authors_institution_code", institution);
        extras.put("data-authors_email", person.getEmail());

        // Default value for those field is an empty string because this will allow us to override the previous set value
        extras.put("data-authors_identifier_fgs", "");
        extras.put("data-authors_identifier_noma", "");
        if(institution.contains("UCL")){
            extras.put("data-authors_identifier_fgs", person.getPrimaryId());
        }
        if(!employeeId.isEmpty()){
            extras.put("data-authors_identifier_noma", employeeId);
            extras.put("authors-identifier-noma", employeeId);
        }
        if(!orcid.getOrcid().isEmpty()){
            // For future usage, metadata reference might need to be changed
            extras.put("data-person_identifier_orcid", orcid.getID());
        }

        return extras;
    }
}
