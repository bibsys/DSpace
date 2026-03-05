/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.publication;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.core.Context;
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

    public static final String SUBTYPE_KEYNOTE = "keynote";
    public static final String SUBTYPE_WITH_SELECTION = "with-selection-speech";
    public static final String SUBTYPE_WITHOUT_SELECTION = "without-selection-speech";
    public static final String SUBTYPE_POSTER = "conference-poster";
    public static final String SUBTYPE_NONE = "none";

    public static final String STATUS_PUBLISHED_SERIAL = "published_in_serial";
    public static final String STATUS_PUBLISHED_BOOK = "published_in_book";
    public static final String STATUS_NOT_PUBLISHED = "not_published";

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
            || itemService.hasMetadata(item, HOST_DOCUMENT_TITLE_FIELD);
    }

    public boolean publishedInSerial() {
        return Objects.equals(getFirstMetadataValue(SPEECH_STATUS_FIELD), STATUS_PUBLISHED_SERIAL);
    }

    public boolean publishedInBook() {
        return Objects.equals(getFirstMetadataValue(SPEECH_STATUS_FIELD), STATUS_PUBLISHED_BOOK);
    }

    public boolean isAbstract() {
        return Objects.equals(getFirstMetadataValue(CONFERENCE_IS_ABSTRACT_FIELD), "true");
    }

    // FWB METHODS IMPLEMENTATION ======================================================================================
    // There is a some specific rules for FWB check about a conference speech. If the conference speech is published
    // (into a book or a serial) after the start decree date, the publication <strong>must</strong> contains an
    // openAccess or an embargo file (1year max)
    @Override
    public Pair<Boolean, String> isFWBCompliant(Context context) {
        LocalDate pubDate = getPublicationDateIssued();
        if (pubDate == null) {
            return VALIDATION_FAILURE_NO_DATE;
        }
        if (isPublished() && pubDate.getYear() >= DECREE_YEAR) {
            try {
                return (itemService.hasUploadedFiles(item))
                    ? validateFWBFileAccess(context)
                    : VALIDATION_FAILURE_NO_FILE;
            } catch (SQLException ignored) {
                return VALIDATION_SUCCESS;
            }
        }
        return VALIDATION_SUCCESS;
    }

    @Override
    public boolean isFWBExportable(Context context) {
        // 1) we export conference speech if publication date > decree date
        // 2) we export conference speech if it's not published
        // 3) we export conference speech compliant with decree
        LocalDate pubDate = getPublicationDateIssued();
        if (pubDate == null) {
            return false;
        }
        boolean isPreDecree = pubDate.getYear() < DECREE_YEAR;
        if (isPreDecree || !isPublished()) {
            return true;
        }
        return isFWBCompliant(context).getLeft();
    }
}
