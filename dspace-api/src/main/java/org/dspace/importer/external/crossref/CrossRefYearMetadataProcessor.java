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
import java.util.Iterator;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Used when we want to extract a year from a date like json node.
 * The json node must have the following form: "key": [[{year}, {month}, {day}]]
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class CrossRefYearMetadataProcessor extends AbstractJsonPathMetadataProcessor {
    /**
     * Extract all the years from the arrays of the provided path.
     * 
     * @param json The json from which to extract the years.
     * @return A list of values to set to the corresponding field.
     */
    @Override
    protected Collection<String> processValues(JsonNode node) {
        Iterator<JsonNode> dates = node.iterator();
        Collection<String> values = new ArrayList<>();
        while (dates.hasNext()) {
            JsonNode date = dates.next();
            if (!date.isNull() && !date.isEmpty() && date.isArray()) {
                // Retrieve the date from the array.
                JsonNode firstEntry = date.get(0);
                if (firstEntry.has(0)) {
                    values.add(firstEntry.get(0).asText());
                }
            }
        }
        return values;
    }
}
