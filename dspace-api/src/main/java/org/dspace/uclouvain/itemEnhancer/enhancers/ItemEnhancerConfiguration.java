/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.enhancers;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.service.MetadataFieldService;
import org.dspace.uclouvain.itemEnhancer.UCLouvainItemEnhancerService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Configuration used by the metadata enhancer service {@link UCLouvainItemEnhancerService}.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ItemEnhancerConfiguration {
    private String sourceEntityType;
    private List<String> sourceMetadataFields;
    private String targetEntityType;
    private List<String> targetMetadataFields;

    @Autowired
    MetadataFieldService metadataFieldService;

    // GETTERS && SETTERS

    public void setSourceEntityType(String type) {
        sourceEntityType = type;
    }

    public String getSourceEntityType() {
        return sourceEntityType;
    }

    public void setSourceMetadataFields(List<String> fields) throws SQLException {
        sourceMetadataFields = fields;
    }

    public List<String> getSourceMetadataFields() {
        return sourceMetadataFields;
    }

    public void setTargetEntityType(String type) {
        targetEntityType = type;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public void setTargetMetadataFields(List<String> fields) throws SQLException {
        targetMetadataFields = fields;
    }

    public List<String> getTargetMetadataFields() {
        return targetMetadataFields;
    }

    /**
     * Checks the validity of the configuration depending on the given types.
     * @param sourceEntityType The entity type of the source item.
     * @param targetEntityType The entity type of the target item.
     * @return True if the current configuration is valid for the given types. False otherwise.
     */
    public boolean isValidForEntityTypes(String sourceEntityType, String targetEntityType) {
        return this.getSourceEntityType().equals(sourceEntityType)
            && this.getTargetEntityType().equals(targetEntityType);
    }
}
