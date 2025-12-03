/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.validation.fnrs.rules;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.Item;

public class MetadataValidationRule extends ValidationRule {

    // CLASS ATTRIBUTES ================================================================================================
    private String metadataField;
    private List<String> validValues;

    // OVERRIDE METHODS ================================================================================================
    @Override
    public boolean validate(Item item) {
        return item
            .getMetadata()
            .stream()
            .filter(m -> m.getMetadataField().toString('.').equals(metadataField))
            .anyMatch(m -> validValues.contains(m.getValue()));
    }

    // GETTER & SETTER =================================================================================================
    public String getMetadataField() {
        return metadataField;
    }
    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }
    public List<String> getValidValues() {
        return validValues;
    }
    public void setValidValues(List<String> validValues) {
        this.validValues = validValues;
    }
    public void setValidValues(String validValues) {
        this.validValues = Arrays.stream(validValues.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());

    }
}
