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
import org.dspace.content.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;

/** Validation rule to check if some metadata exists into an `Item`
 *    If multiple metadata fields are defined, any metadata belonging to the item matching metadata field will validate
 *    this rule.
 *    Into DSpace, a metadata value cannot be empty or blank string; So no need to check if the item metadata value
 *    is present, only to check the presence of the metadata field.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class ExistsMetadataValidationRule extends ValidationRule {

    // CLASS ATTRIBUTES ================================================================================================
    @Autowired
    private ItemService itemService;
    private List<String> metadataFields;

    // OVERRIDE METHODS ================================================================================================
    @Override
    public boolean validate(Item item) {
        return metadataFields.stream().anyMatch(mdString -> itemService.hasMetadata(item, mdString));
    }

    // GETTERS & SETTERS ===============================================================================================
    public List<String> getMetadataFields() {
        return metadataFields;
    }
    public void setMetadataFields(List<String> metadataFields) {
        this.metadataFields = metadataFields;
    }
    public void setMetadataFields(String metadataFields) {
        this.metadataFields = Arrays.stream(metadataFields.split(","))
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

}
