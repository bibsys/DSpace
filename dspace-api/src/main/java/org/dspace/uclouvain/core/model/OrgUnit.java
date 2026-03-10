/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model;

import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;

/**
 * Object representing an OrgUnit object.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class OrgUnit extends ItemModel {

    // CLASS CONSTANTS =================================================================================================
    public static final String ENTITY_TYPE = "OrgUnit";

    public static final String RESEARCH_INSTITUTE = "Research Institute";
    public static final String UNIVERSITY = "University";

    public static final int DEFAULT_WEIGHT = 50;

    // METADATA FIELDS DEFINITIONS =====================================================================================
    public static final String TITLE_FIELD = getField("title", "dc.title");
    public static final String TYPE_FIELD = getField("type", "dc.type");
    public static final String ACRONYM_FIELD = getField("acronym", "oairecerif.acronym");
    public static final String IS_SELECTABLE_FIELD = getField("isSelectable", "organization.isSelectable");
    public static final String WEIGHT_FIELD = getField("weight", "organization.weight");

    // CLASS ATTRIBUTES ================================================================================================
    private OrgUnit parent;
    private OrgUnit parentUniversity;

    // CONSTRUCTOR =====================================================================================================
    public OrgUnit(Item item) throws InvalidModelEntityTypeException {
        super(item);
        if (!Objects.equals(itemService.getEntityType(item), ENTITY_TYPE)) {
            throw new InvalidModelEntityTypeException(item, ENTITY_TYPE);
        }
    }

    // GETTER ==========================================================================================================
    public String getType() {
        return getFirstMetadataValue(TYPE_FIELD);
    }
    public String getAcronym() {
        return getFirstMetadataValue(ACRONYM_FIELD);
    }
    public boolean isSelectable() {
        return Boolean.parseBoolean(getFirstMetadataValue(IS_SELECTABLE_FIELD));
    }
    public int getWeight() {
        try {
            return Integer.parseInt(getFirstMetadataValue(WEIGHT_FIELD));
        } catch (NumberFormatException e) {
            return DEFAULT_WEIGHT;
        }
    }

    // FUNCTIONS =======================================================================================================
    /**
     * Allows retrieving the direct parent ancestor of this OrgUnit
     * @param useCache if the parent is already loaded, use this cached value.
     * @return the parent OrgUnit; null if the current OrgUnit doesn't have any parent
     */
    public OrgUnit getParent(boolean useCache) {
        if (useCache && parent != null) {
            return parent;
        }
        MetadataValue parentOrg = itemService
            .getMetadataByMetadataString(item, "organization.parentOrganization")
            .stream()
            .findFirst()
            .orElse(null);
        if (parentOrg != null && !StringUtils.isBlank(parentOrg.getAuthority())) {
            try {
                Item parentItem = itemService.find(context, UUID.fromString(parentOrg.getAuthority()));
                return parent = new OrgUnit(parentItem);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
    public OrgUnit getParent() {
        return getParent(true);
    }

    /**
     * Allows retrieving the closer parent ancestor of this OrgUnit with `University` type
     * @param useCache if the parent is already loaded, use this cached value.
     * * @return the university parent OrgUnit; null if the current OrgUnit doesn't have any university parent
     */
    public OrgUnit getParentUniversity(boolean useCache) {
        if (useCache && parentUniversity != null) {
            return parentUniversity;
        }
        OrgUnit parent = getParent();
        if (parent == null) {
            return null;
        }
        if (parent.getType().equals(UNIVERSITY)) {
            return parentUniversity = parent;
        }
        return parent.getParentUniversity(useCache);
    }
    public OrgUnit getParentUniversity() {
        return getParentUniversity(true);
    }

    /**
     * Get the metadata field string from a configuration key.
     * If no config is found for the given key, use given default value.
     * 
     * @param fieldName The key of the metadatafield configuration to find.
     * @param defaultValue The default value to use in case the config is not found.
     * @return The value of the config key or default value if not found.
     */
    private static String getField(String fieldName, String defaultValue) {
        return configService.getProperty(
            "%sorgUnit.%s.field".formatted(FIELD_PREFIX, fieldName),
            defaultValue);
    }

}

