package org.dspace.uclouvain.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.dspace.content.MetadataFieldName;

/**
 * Little class that extends a metadata field reference string.
 */
public class MetadataField extends MetadataFieldName {

    public MetadataField(String metadataFieldName) throws Exception {
        super(metadataFieldName);
    }

    public String getSchema() {
        return this.schema;
    }

    public String getElement() {
        return this.element;
    }

    public String getQualifier() {
        return this.qualifier;
    }

    /**
     * Retrieve the full string for the metadata field name based on a given separator.
     * 
     * @param separator: The symbol used to recreate the string.
     * @return The full string containing the schema, element and qualifier.
     */
    public String getFullString(String separator) {
        // We need to use `new ArrayList<>()` for the instantiation since it will allow us to use the `add()` method on the list.
        List<String> sections = new ArrayList<>(Arrays.asList(this.schema, this.element));
        if (this.qualifier != null) {
            sections.add(this.qualifier);
        }
        return String.join(separator, sections);
    }

    /**
     * @return The full string containing the schema, element and qualifier linked with a dot.
     */
    public String getFullString(){
        return this.getFullString(".");
    }

    @Override
    public String toString() {
        return this.getFullString();
    }
}
