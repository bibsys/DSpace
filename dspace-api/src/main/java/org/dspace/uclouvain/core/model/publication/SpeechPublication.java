/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.publication;

import org.dspace.content.Item;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;

/**
 * Object representing a Conference speech publication object.
 * With some specific method concerning conference output metadata.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class SpeechPublication extends Publication {

    // METADATA FIELDS DEFINITIONS =====================================================================================
    public static final String DOCUMENT_TYPE = "text::conference-speech";

    public static final String CONFERENCE_NAME_FIELD = configService.getProperty(
            FIELD_PREFIX + "publication.conferenceName.field",
            "publication.conference.name");
    public static final String CONFERENCE_LOCATION_FIELD = configService.getProperty(
            FIELD_PREFIX + "publication.conferenceLocation.field",
            "publication.conference.location");
    public static final String CONFERENCE_START_DATE_FIELD = configService.getProperty(
            FIELD_PREFIX + "publication.conferenceStartDate.field",
            "publication.conference.startDate");
    public static final String CONFERENCE_END_DATE_FIELD = configService.getProperty(
            FIELD_PREFIX + "publication.conferenceEndDate.field",
            "publication.conference.endDate");
    private static final String JOURNAL_TITLE_FIELD = configService.getProperty(
            FIELD_PREFIX + "journal.field",
            "dc.relation.journal");
    private static final String HOST_BOOK_TITLE_FIELD = configService.getProperty(
            FIELD_PREFIX + "hosttitle.field",
            "publication.host.title"
    );

    // CONSTRUCTOR =====================================================================================================
    protected SpeechPublication(Item item) throws InvalidModelEntityTypeException {
        super(item);
    }

    // FUNCTIONS =======================================================================================================
    public String getRawConferenceStartDate() {
        return getFirstMetadataValue(CONFERENCE_START_DATE_FIELD);
    }
    public String getRawConferenceEndDate() {
        return getFirstMetadataValue(CONFERENCE_END_DATE_FIELD);
    }
    public String getConferenceName() {
        return getFirstMetadataValue(CONFERENCE_NAME_FIELD);
    }
    public String getConferenceLocation() {
        return getFirstMetadataValue(CONFERENCE_LOCATION_FIELD);
    }

    public boolean isPublished() {
        return itemService.hasMetadata(item, JOURNAL_TITLE_FIELD)
            || itemService.hasMetadata(item, HOST_BOOK_TITLE_FIELD);
    }
}
