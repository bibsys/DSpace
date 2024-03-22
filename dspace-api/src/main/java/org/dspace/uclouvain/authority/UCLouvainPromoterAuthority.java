package org.dspace.uclouvain.authority;

import java.util.HashMap;
import java.util.Map;

import org.dspace.uclouvain.authority.client.UCLouvainAuthorityClient;
import org.dspace.uclouvain.authority.factory.UCLouvainServiceFactory;
import org.dspace.uclouvain.external.dilbert.model.DialPerson;

public class UCLouvainPromoterAuthority extends AbstractUCLouvainAuthority {

    private UCLouvainAuthorityClient getUCLouvainAuthorityClient() {
        return UCLouvainServiceFactory.getInstance().getUCLouvainAuthorityClient();
    }

    public String getLabel(String key, String locale) {
        return "";
    }

    /** 
     * Retrieve the list of promoter based on the query (first and last name)
     * 
     * @param query: The name typed by the user, used to search for students
     * @return: An array containing the result of the search  
     */
    DialPerson[] getSuggestions(String query) {
        return this.getUCLouvainAuthorityClient().getSuggestionByTermWithFilter(query, "promoters");
    }

    /** 
     * Generate a list of extra information that will be displayed under the main information 
     * Also used to populate the fields when the user selects a choice in the list
     * 
     * @param DialPerson: One dialPerson (result from a search) that will be used to generate extra data
     * @return: An array containing the extras 
     */
    Map<String, String> generateExtras(DialPerson person) {
        Map<String, String> extras = new HashMap<>();

        String entity = person.getEntity();
        String institution = person.getInstitution();

        if (!entity.isEmpty()) extras.put("institution-entity-name", entity);

        // String with "data-" will be used to fill other form fields 
        extras.put("data-advisors_email", person.getEmail());
        extras.put("data-advisors_institution_name", institution);

        return extras;
    }
}
