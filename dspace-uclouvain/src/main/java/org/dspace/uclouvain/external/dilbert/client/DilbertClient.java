package org.dspace.uclouvain.external.dilbert.client;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.dspace.uclouvain.external.dilbert.model.DialPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.uclouvain.authority.client.UCLouvainAuthorityClient;
import org.dspace.uclouvain.authority.configuration.UCLouvainAuthorityAPIConfiguration;
import org.dspace.uclouvain.core.GenericHttpClient;
import org.dspace.uclouvain.core.GenericResponse;

@Service
public class DilbertClient implements UCLouvainAuthorityClient {

    private static Logger logger = LogManager.getLogger(DilbertClient.class);

    private UCLouvainAuthorityAPIConfiguration uclouvainAuthorityConfiguration;
    private GenericHttpClient httpClient;

    /** 
     * Base UCLouvainClient Constructor, instantiate HttpClient & UCLouvainAuthorityConfiguration.
     */
    @Autowired
    public DilbertClient(UCLouvainAuthorityAPIConfiguration uclouvainAuthorityConfiguration) {
        this.uclouvainAuthorityConfiguration = uclouvainAuthorityConfiguration;
    }

    /** 
     * Generic method to call the Dilbert API with a given term && filter.
     * 
     * @param term: The name and/or second name of the person.
     * @param filterKey: The filter key which indicate the type of person to extract (ex: student, promoter...)
     * @return: A list of dial person information.
     */
    public DialPerson[] getSuggestionByTermWithFilter(String term, String filterKey) {
        String filter = "&filter=" + uclouvainAuthorityConfiguration.getFilterByKey(filterKey);
        String cleanedTerm = this.encodeURL(term);
        String url = "/searchAuthorDrupal.php?term=" + cleanedTerm + filter;

        DialPerson[] dialPerson = {};

        try {
            HttpResponse<String> response = httpClient.get(url);
            dialPerson = new GenericResponse(response.body()).extractJsonResponseDataToClass(null, DialPerson[].class);
        }
        catch(Exception e) {
            logger.error(e.getClass().getSimpleName() + "while fetching data :: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return dialPerson;
    }

    // Utils
    /**
     * Encode URL to handle escaping characters like spaces.
     * 
     * @param url: The string url to encode.
     */
    private String encodeURL(String url) {
        try {
            return URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
        }
        catch(UnsupportedEncodingException e) {
            logger.error("Error while encodingURL ::" + e.getMessage());
            throw new RuntimeException("Cannot encode: " + url);
        }
    }

    // Setters && getters 
    public void setHttpClient(GenericHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public GenericHttpClient getHttpClient(){
        return this.httpClient;
    }
}
