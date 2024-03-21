package org.dspace.uclouvain.core;

import java.io.IOException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 
* Represent a basic Http Response from java.net.http lib
* Used to access the JSON response
*/
public class GenericResponse {

    private String responseBody;
    // From Jackson, used to deserialize JSON to java 
    private ObjectMapper objectMapper = new ObjectMapper();
    
    /** 
     * Base GenericResponse constructor
     */
    public GenericResponse(String response) {
        this.responseBody = response;
    }

    /** 
     * Extract the json content to an instance of a class
     * Usually, in the OSIS API, the real content is at the "return" key in the JSON response
     * 
     * @param targetKey
     * @param clazz
     * @return T
     * @throws IOException
     */
    public <T> T extractJsonResponseDataToClass(String targetKey, Class<T> clazz) throws IOException {
        this.objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        JsonNode cleanedJson = this.objectMapper.readTree(responseBody);
        // If set, find the "targetKey" element's value
        if (targetKey != null) {
            cleanedJson = cleanedJson.findValue(targetKey);
        }
        // Convert JSON String into a java object
        return this.objectMapper.treeToValue(cleanedJson, clazz);
    }
}
