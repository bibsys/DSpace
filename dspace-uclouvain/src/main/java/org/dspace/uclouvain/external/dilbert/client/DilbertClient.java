package org.dspace.uclouvain.external.dilbert.client;

import java.io.IOException;
import java.net.http.HttpResponse;

import org.dspace.uclouvain.external.dilbert.model.DialPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.dspace.uclouvain.authority.client.UCLouvainAuthorAuthorityClient;
import org.dspace.uclouvain.authority.configuration.UCLouvainAuthorAuthorityAPIConfiguration;
import org.dspace.uclouvain.core.GenericHttpClient;
import org.dspace.uclouvain.core.GenericResponse;

@Service
public class DilbertClient implements UCLouvainAuthorAuthorityClient {

    private UCLouvainAuthorAuthorityAPIConfiguration uclouvainAuthorityConfiguration;
    private GenericHttpClient httpClient;

    /** 
     * Base UCLouvainClient Constructor, instantiate HttpClient & UCLouvainAuthorityConfiguration
     */
    @Autowired
    public DilbertClient(UCLouvainAuthorAuthorityAPIConfiguration uclouvainAuthorityConfiguration) {
        this.uclouvainAuthorityConfiguration = uclouvainAuthorityConfiguration;
    }
    
    /** 
     * Search by student first name or/and second name and filtering by {filter} (ex: filter="student")
     * It returns a list of student information
     * 
     * @param term
     * @param filter
     * @return: A list of dial peron information 
     */
    public DialPerson[] getStudentByTermWithFilter(String term) {
        // GET {apiUrl}/searchAuthorDrupal.php?term={term}&filter={filter}
        String filter = "&filter=" + uclouvainAuthorityConfiguration.getFilter();
        String url = "/searchAuthorDrupal.php?term=" + term + filter;

        DialPerson[] dialPerson = {};

        try {
            HttpResponse<String> response = httpClient.get(url);
            dialPerson = new GenericResponse(response).extractJsonResponseDataToClass(null, DialPerson[].class);
        }
        catch(IOException | InterruptedException e) {
            System.err.println(e.getClass().getSimpleName() + "while fetching data :: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return dialPerson;
    }

    // Setters && getters 
    public void setHttpClient(GenericHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public GenericHttpClient getHttpClient(){
        return this.httpClient;
    }
}
