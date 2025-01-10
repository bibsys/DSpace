/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.JsonPathMetadataProcessor;

/**
 * Abstract class for a basic JsonPathMetadataProcessor.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public abstract class AbstractJsonPathMetadataProcessor implements JsonPathMetadataProcessor {
    private final static Logger logger = LogManager.getLogger();
    protected String pathToArray;

    abstract protected Collection<String> processValues(JsonNode node);

    /**
     * Retrieve a json node form the json tree using the given 'pathToArray'.
     * Then pass the node to the abstract method to retrieve a collection of value to return.
     * 
     * @param jsonTree The json tree in a String representation.
     * @return The extracted values in a collection.
     */
    @Override
    public Collection<String> processMetadata(String jsonTree) {
        try {
            JsonNode rootNode = new ObjectMapper().readTree(jsonTree);
            return processValues(rootNode.at(pathToArray));
        } catch (JsonProcessingException e) {
            logger.error("Unable to process json response.", e);
            return Collections.emptyList();
        }
    }

    // GETTERS && SETTERS
    public void setPathToArray(String path) {
        pathToArray = path;
    }

    public String getPathToArray() {
        return pathToArray;
    }
}
