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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;

/**
 * Object representing a journal article object (text::journal-article).
 * With some specific method concerning article metadata.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class ArticlePublication extends Publication {

    // METADATA FIELDS DEFINITIONS =====================================================================================
    public static final String DOCUMENT_TYPE = "text::journal-article";

    public static final String SUBTYPE_POPULARISING_ARTICLE = "popularising-article";
    public static final String SUBTYPE_FULL_ISSUE = "full-issue";
    public static final String SUBTYPE_RESEARCH_ARTICLE = "research-article";
    public static final String SUBTYPE_REPORT = "report";
    public static final String SUBTYPE_EDITORIAL = "editorial";
    public static final String SUBTYPE_CLINICAL_STUDY = "clinical-study";
    public static final String SUBTYPE_LETTER_TO_THE_EDITOR = "letter-to-the-editor";
    public static final String SUBTYPE_LAW_CASE_NOTE = "law-case-note";
    public static final String SUBTYPE_LITERATURE_REVIEW = "literature-review";
    public static final String SUBTYPE_FEATURE_ARTICLE = "feature-article";
    public static final String SUBTYPE_NONE = "none";

    private static final String JOURNAL_TITLE_FIELD = configService.getProperty(
            FIELD_PREFIX + "journal.field", "dc.relation.journal");
    private static final String JOURNAL_PUBSTATUS_FIELD = configService.getProperty(
            FIELD_PREFIX + "publicationstatus.field", "publication.publicationStatus");
    private static final String JOURNAL_PEERREVIEW_FIELD = configService.getProperty(
            FIELD_PREFIX + "publication.journal.peer-review.field", "publication.serial.peerReviewed");


    // CONSTRUCTOR =====================================================================================================
    protected ArticlePublication(Item item) throws InvalidModelEntityTypeException {
        super(item);
    }

    // FUNCTIONS =======================================================================================================
    public boolean isPublished() {
        return List.of(Publication.STATUS_INPRESS, Publication.STATUS_PUBLISHED).contains(getPublicationStatus());
    }
    public String getPublicationStatus() {
        return this.getFirstMetadataValue(JOURNAL_PUBSTATUS_FIELD);
    }
    public String getJournalTitle() {
        return this.getFirstMetadataValue(JOURNAL_TITLE_FIELD);
    }
    public boolean isPeerReviewed() {
        return Objects.equals(this.getFirstMetadataValue(JOURNAL_PEERREVIEW_FIELD), "true");
    }

    // FWB METHODS IMPLEMENTATION ======================================================================================
    // There is a some specific rules for FWB check about a journal article:
    //   * if the article is not yet enter into publishing process, compliance will be OK but must not be exported !
    //   * if the article subtype has "editorial" or "popularizing-article" value, compliance OK, export NO
    //   * other case are compliant (and exportable) if the publication date is after the start decree date and if
    //     the publication contains an openAccess or an embargo file (1year max)
    @Override
    public Pair<Boolean, String> isFWBCompliant(Context context) {
        LocalDate pubDate = getPublicationDateIssued();
        if (pubDate == null) {
            return VALIDATION_FAILURE_NO_DATE;
        }
        boolean isPreDecree = pubDate.getYear() < DECREE_YEAR;
        boolean isExemptType = Arrays.asList(SUBTYPE_POPULARISING_ARTICLE, SUBTYPE_FULL_ISSUE).contains(getSubType());
        boolean isPublished = Arrays.asList(STATUS_INPRESS, STATUS_PUBLISHED).contains(getPublicationStatus());
        if (isPreDecree || isExemptType || !isPublished) {
            return VALIDATION_SUCCESS;
        }
        try {
            return itemService.hasUploadedFiles(item)
                ? validateFWBFileAccess(context)
                : VALIDATION_FAILURE_NO_FILE;
        } catch (SQLException ignored) {
            return VALIDATION_SUCCESS;
        }
    }

    @Override
    public boolean isFWBExportable(Context context) {
        // 1) we export article if publication date < decree date
        // 2) we export not yet published (or at least in-press) article.
        // 3) we export "popularizing" or "full-issue" article
        // 4) we export article compliant with FWB rules
        LocalDate pubDate = getPublicationDateIssued();
        if (pubDate == null) {
            return false;
        }
        boolean isPreDecree = pubDate.getYear() < DECREE_YEAR;
        boolean isExemptType = Arrays.asList(SUBTYPE_POPULARISING_ARTICLE, SUBTYPE_FULL_ISSUE).contains(getSubType());
        boolean isPublished = Arrays.asList(STATUS_INPRESS, STATUS_PUBLISHED).contains(getPublicationStatus());
        if (isPreDecree || isExemptType || !isPublished) {
            return true;
        }
        return isFWBCompliant(context).getLeft();
    }
}
