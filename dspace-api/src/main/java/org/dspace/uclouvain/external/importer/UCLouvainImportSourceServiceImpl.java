/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.importer;

import static org.dspace.content.authority.Choices.CF_UNSET;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.CrisConstants;
import org.dspace.uclouvain.core.model.MetadataField;

public abstract class UCLouvainImportSourceServiceImpl implements UCLouvainImportSourceService {
    public abstract List<MetadataValueDTO> getMetadataList(String query);

    /**
     * Adds a metadata to the given metadata list.
     * If the placeholder flag is set to true and the value is empty, add a metadata placeholder.
     * If the placeholder flag is set to false and the value is empty, don't add any metadata.
     * 
     * @param list The complete metadata list to amend the metadata to.
     * @param field The metadata field of the metadata to create.
     * @param value The value of the metadata to create.
     * @param authority The authority of the metadata to create.
     * @param confidence The confidence of the metadata to create.
     * @param placeholder Whether to use a placeholder for the value of the metadata if given value is empty.
     */
    protected void addMetadata(
        List<MetadataValueDTO> list, String field, String value, String authority, int confidence, boolean placeholder
    ) {
        boolean isBlank = StringUtils.isEmpty(value);
        if (isBlank && !placeholder) {
            return;
        }

        value = isBlank ? CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE : value;
        authority = isBlank ? null : authority;
        confidence = isBlank ? CF_UNSET : confidence;

        MetadataField md = new MetadataField(field);
        MetadataValueDTO dto = new MetadataValueDTO();

        dto.setSchema(md.getSchema());
        dto.setElement(md.getElement());
        dto.setQualifier(md.getQualifier());
        dto.setValue(value);
        dto.setAuthority(authority);
        dto.setConfidence(confidence);

        list.add(dto);
    }

    /**
     * Same as 'addMetadata' but for a list of values.
     * @param list The complete metadata list to amend the metadata to.
     * @param field The metadata field of the metadata to create.
     * @param values The values of the metadata to create.
     * @param authority The authority of the metadata to create.
     * @param confidence The confidence of the metadata to create.
     * @param placeholder Whether to use a placeholder for the value of the metadata if given value is empty.
     */
    protected void addAllMetadata(
        List<MetadataValueDTO> list, String field, List<String> values,
        String authority, int confidence, boolean placeholder
    ) {
        if (values == null) {
            return;
        }
        values.forEach(val -> addMetadata(list, field, val, authority, confidence, placeholder));
    }
}
