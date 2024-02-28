package org.dspace.uclouvain.core.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Little class that represent a metadata field reference string.
 */
public class MetadataField {
    private String schema;
    private String element;
    private String qualifier;

    public MetadataField(String metadataFieldName, String separator) throws Exception {
        List<String> splitted = Arrays.asList(metadataFieldName.split(separator));

        if (splitted.size() < 2) {
            throw new Exception("Error while splitting the metadata field with separator: '" + separator + "'");
        }
        this.schema = splitted.get(0);
        this.element = splitted.get(1);
        if (splitted.size() > 2) {
            this.qualifier = splitted.get(2);
        }
    }

    public MetadataField(String metadataFieldName) throws Exception {
        this(metadataFieldName, "\\.");
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
