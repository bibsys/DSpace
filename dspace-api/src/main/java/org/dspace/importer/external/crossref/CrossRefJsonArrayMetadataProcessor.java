/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base processor to extract a value from an array of nodes.
 * This is handy if you wish to extract a value from a specific node
 * in an array that has a given property value.
 * 
 * For example, if we have the following json array:
 * [{
 *  "name": "author_name",
 *  "value" "Mikel Theunis"
 * },
 * {
 *  "name": "author_email",
 *  "value": "theunis.mikel@test.com"
 * }]
 * Using this processor as a bean and passing 'name' as "nodeKeyToCheck", 'author_name' as "nodeValueToCheck"
 * and 'value' as "nodeKeyToExtract", you could extract the value 'Mikel Theunis' from this array of nodes.
 * 
 * Important to note that this class only works on json arrays.
 * 
 * @author: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class CrossRefJsonArrayMetadataProcessor extends AbstractJsonPathMetadataProcessor {
    protected String nodeKeyToCheck;
    protected String nodeValueToCheck;
    protected String nodeKeyToExtract;

    /**
     * Main method called to extract the desired value based on the given 'parameters'.
     * 
     * @param node The main json node array to browse.
     * @return The value of the desired Key if the given conditions where met. Else could return
     * an empty Collection of string.
     */
    @Override
    protected Collection<String> processValues(JsonNode node) {
        // Make sure that the root node is not empty and is an array.
        if (node.isNull() || node.isEmpty() || !node.isArray()) {
            return Collections.emptyList();
        }

        // Now we know that we have an array of nodes.
        // Search this array to find a node element that has a correct key and value based on the given params.

        Collection<String> values = new ArrayList<>();
        // Create an iterator containing array node children and loop over them.
        Iterator<JsonNode> childNodes = node.iterator();
        while (childNodes.hasNext()) {
            JsonNode childNode = childNodes.next();
            // Get the desired field from the JsonNode child.
            JsonNode targetNode = childNode.get(nodeKeyToCheck);
            if (!targetNode.isNull() && targetNode.asText().equals(nodeValueToCheck)) {
                // Add the desired key value to collection.
                values.add(childNode.get(nodeKeyToExtract).asText());
            }
        }

        return values;
    }

    // SETTERS FOR ATTRIBUTES
    public void setNodeKeyToCheck(String nodeKeyToCheck) {
        this.nodeKeyToCheck = nodeKeyToCheck;
    }

    public void setNodeValueToCheck(String nodeValueToCheck) {
        this.nodeValueToCheck = nodeValueToCheck;
    }

    public void setNodeKeyToExtract(String nodeKeyToExtract) {
        this.nodeKeyToExtract = nodeKeyToExtract;
    }
}