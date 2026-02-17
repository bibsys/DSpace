/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model;

import java.util.Objects;

import org.dspace.content.Item;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;

/**
 * This model represents a journal object.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class Journal extends ItemModel {

    // CLASS CONSTANTS =================================================================================================
    public static final String ENTITY_TYPE = "Journal";
    public static final String ACTIVE_ACCESS_TYPE = "active";
    public static final String CEASED_ACCESS_TYPE = "ceased";

    public static final String ISSN_IDENTIFIER = "issn";
    public static final String EISSN_IDENTIFIER = "eissn";

    // METADATA FIELDS DEFINITIONS =====================================================================================
    public static final String TITLE_FIELD = getField("title", "dc.title");
    public static final String ISSN_FIELD = getField("issn", "dc.identifier.issn");
    public static final String EISSN_FIELD = getField("eissn", "dc.identifier.eissn");
    private static final String PUBLISHER_NAME_FIELD = getField("publisherName", "dc.publisher");
    private static final String PUBLISHER_LOCATION_FIELD = getField("publisherLocation", "dc.publisher.location");
    private static final String PEER_REVIEWED_FIELD = getField("peerReviewed", "journal.peerReviewed");
    private static final String STATUS_CODE_FIELD = getField("statusCode", "journal.statusCode");

    // CONSTRUCTOR =====================================================================================================
    public Journal(Item item) throws InvalidModelEntityTypeException {
        super(item);
        if (!Objects.equals(itemService.getEntityType(item), ENTITY_TYPE)) {
            throw new InvalidModelEntityTypeException(item, ENTITY_TYPE);
        }
    }

    // GETTER ==========================================================================================================
    public String getEntityType() {
        return ENTITY_TYPE;
    }

    public String getTitle() {
        return getFirstMetadataValue(TITLE_FIELD);
    }

    public String getIdentifier(String idType) {
        return switch (idType) {
            case ISSN_IDENTIFIER -> getFirstMetadataValue(ISSN_FIELD);
            case EISSN_IDENTIFIER -> getFirstMetadataValue(EISSN_FIELD);
            default -> throw new IllegalArgumentException(idType + " is not a valid identifier");
        };
    }

    public String getPublisher() {
        return getFirstMetadataValue(PUBLISHER_NAME_FIELD);
    }

    public String getPublisherLocation() {
        return getFirstMetadataValue(PUBLISHER_LOCATION_FIELD);
    }

    public boolean isPeerReviewed() {
        return Boolean.parseBoolean(getFirstMetadataValue(PEER_REVIEWED_FIELD));
    }

    public String isPeerReviewedString() {
        return getFirstMetadataValue(PEER_REVIEWED_FIELD);
    }

    public String getStatusCode() {
        return getFirstMetadataValue(STATUS_CODE_FIELD);
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
            "%sjournal.%s.field".formatted(FIELD_PREFIX, fieldName),
            defaultValue);
    }
}