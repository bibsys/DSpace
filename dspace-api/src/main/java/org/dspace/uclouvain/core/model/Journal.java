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
    public static final String TITLE_FIELD =
            configService.getProperty(FIELD_PREFIX + "title.field", "dc.title");
    public static final String ISSN_FIELD =
            configService.getProperty(FIELD_PREFIX + "journalissn.field", "dc.identifier.issn");
    public static final String EISSN_FIELD =
            configService.getProperty(FIELD_PREFIX + "journaleissn.field", "dc.identifier.eissn");
    private static final String PUBLISHER_NAME_FIELD =
            configService.getProperty(FIELD_PREFIX + "journal.publisher.name.field", "dc.publisher");
    private static final String PUBLISHER_LOCATION_FIELD =
        configService.getProperty(FIELD_PREFIX + "journal.publisher.location.field", "dc.publisher.location");
    private static final String PEER_REVIEWED_FIELD =
        configService.getProperty(FIELD_PREFIX + "journal.peerreviewed.field", "dc.description.peerreviewed");
    private static final String STATUS_CODE_FIELD =
            configService.getProperty(FIELD_PREFIX + "journal.statuscode.field", "dc.description.status");

    // CONSTRUCTOR =====================================================================================================
    public Journal(Item item) throws InvalidModelEntityTypeException {
        super(item);
        if (!Objects.equals(itemService.getEntityType(item), ENTITY_TYPE)) {
            throw new InvalidModelEntityTypeException(item, ENTITY_TYPE);
        }
    }

    // GETTER ==========================================================================================================
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

    public String getStatusCode() {
        return getFirstMetadataValue(STATUS_CODE_FIELD);
    }
}