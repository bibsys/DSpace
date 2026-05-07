/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.importer.json.crossref;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.content.authority.Choices.CF_UNSET;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.profile.ResearcherProfile;
import org.dspace.uclouvain.core.model.Journal;
import org.dspace.uclouvain.core.model.publication.ArticlePublication;
import org.dspace.uclouvain.core.model.publication.BookChapterPublication;
import org.dspace.uclouvain.core.model.publication.BookPublication;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.ReportPublication;
import org.dspace.uclouvain.core.model.publication.SpeechPublication;
import org.dspace.uclouvain.core.utils.DateUtils;
import org.dspace.uclouvain.external.importer.json.UCLouvainJSONImportSourceService;
import org.dspace.uclouvain.services.JournalService;
import org.dspace.uclouvain.services.UCLouvainProfileService;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to extract a list of MetadataValueDTO from crossRef for a given DOI.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainCrossRefImportSourceService extends UCLouvainJSONImportSourceService {
    @Autowired
    private LiveImportClient liveImportClient;
    @Autowired
    private UCLouvainProfileService uclouvainProfileService;
    @Autowired
    private JournalService journalService;

    private String url;

    @Override
    public List<MetadataValueDTO> getMetadataList(String query) {
        Context context = ContextUtil.obtainCurrentRequestContext();
        String rawResponse = fetchData(query);
        ReadContext parsedJson = parseJsonResponse(rawResponse);
        return generateMetadataList(context, parsedJson);
    }

    private String fetchData(String query) {
        String finalUrl = url + "/" + query;
        Map<String, Map<String, String>> params = new HashMap<>();
        String response = liveImportClient.executeHttpGetRequest(2000, finalUrl, params);
        return response;
    }

    private ReadContext parseJsonResponse(String rawResponse) {
        ReadContext root = JsonPath.parse(rawResponse);
        Configuration conf = Configuration.builder()
            // This is important: return null if a path is not found instead of throwing an exception.
            .options(Option.DEFAULT_PATH_LEAF_TO_NULL, Option.SUPPRESS_EXCEPTIONS)
            .build();

        return JsonPath.using(conf).parse((Object) root.read("$.message"));
    }

    // METADATA EXTRACTION =============================================================================================

    /**
     * Generate a list of all metadata values extracted from the external source.
     * 
     * @param context The current Dspace application context.
     * @param rootJson The root json element to extract data from.
     * @return A list of all extracted MetadataValueDTO.
     */
    private List<MetadataValueDTO> generateMetadataList(Context context, ReadContext rootJson) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();
        if (rootJson == null) {
            return mdValues;
        }

        // Extract main-type of document and map it to correct value.
        String mainType = mapMainType(rootJson.read("$.type"));
        addMetadata(mdValues, Publication.MAIN_TYPE_FIELD, mainType, null, CF_UNSET, false);

        mdValues.addAll(extractAuthors(context, rootJson));
        mdValues.addAll(extractBasicInfo(context, rootJson));
        if (mainType == null) {
            return mdValues;
        }
        // Depending on the document type, we extract additional information.
        switch (mainType) {
            case BookChapterPublication.DOCUMENT_TYPE -> mdValues.addAll(extractBookChapterInfo(context, rootJson));
            case BookPublication.DOCUMENT_TYPE -> mdValues.addAll(extractBookInfo(context, rootJson));
            case ArticlePublication.DOCUMENT_TYPE -> mdValues.addAll(extractArticleInfo(context, rootJson));
            case SpeechPublication.DOCUMENT_TYPE -> mdValues.addAll(extractSpeechInfo(context, rootJson));
            default -> { }
        };

        return mdValues;
    }

    // Metadata extraction methods =====================================================================================

    /**
     * Extract authors information from the root json node.
     * 
     * @param context The current DSpace application context.
     * @param root The root json node to extract data from.
     * @return A list of MetadataValueDTO containing all extracted authors information.
     */
    private List<MetadataValueDTO> extractAuthors(Context context, ReadContext root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();
        List<Map<String, String>> authors = root.read("$.author");
        if (authors == null || authors.isEmpty()) {
            return mdValues;
        }

        if (authors.size() > authorLimit) {
            authors = authors.subList(0, authorLimit);
            addMetadata(mdValues, Publication.AUTHOR_ETAL_FIELD, "true", null, CF_UNSET, false);
        }

        for (Map<String, String> author : authors) {
            String orcid = extractOrcid(author.get("ORCID"));
            Item profileItem = (orcid != null) ? uclouvainProfileService.findByOrcid(context, orcid) : null;
            if (profileItem != null) {
                String authority = profileItem.getID().toString();
                ResearcherProfile profile = new ResearcherProfile(profileItem, false);
                String fullName = profile.getName().orElse(null);
                String officialEmail = profile.getEmail().orElse(null);
                String institution = profile.getInstitution().orElse(null);
                String fgs = profile.getFGS().orElse(null);
                addMetadata(mdValues, Publication.AUTHOR_NAME_FIELD, fullName, authority, CF_ACCEPTED, true);
                addMetadata(mdValues, Publication.AUTHOR_EMAIL_FIELD, officialEmail, authority, CF_ACCEPTED, true);
                addMetadata(mdValues, Publication.AUTHOR_ORCID_FIELD, orcid, authority, CF_ACCEPTED, true);
                addMetadata(mdValues, Publication.AUTHOR_INSTITUTION_FIELD, institution, authority, CF_ACCEPTED, true);
                addMetadata(mdValues, Publication.AUTHOR_FGS_FIELD, fgs, authority, CF_ACCEPTED, true);
                continue;
            }

            String firstName = author.get("given");
            String lastName = author.get("family");
            String fullName = "%s, %s".formatted(lastName, firstName);
            List<String> institutions = JsonPath.parse(author).read("$.affiliation[*].name");
            String institution = institutions.isEmpty() ? null : institutions.get(0);

            addMetadata(mdValues, Publication.AUTHOR_NAME_FIELD, fullName, null, CF_UNSET, true);
            addMetadata(mdValues, Publication.AUTHOR_EMAIL_FIELD, null, null, CF_UNSET, true);
            addMetadata(mdValues, Publication.AUTHOR_ORCID_FIELD, orcid, null, CF_UNSET, true);
            addMetadata(mdValues, Publication.AUTHOR_INSTITUTION_FIELD, institution, null, CF_UNSET, true);
            addMetadata(mdValues, Publication.AUTHOR_FGS_FIELD, null, null, CF_UNSET, true);
        }
        return mdValues;
    }

    /**
     * Extract basic information from the root json node.
     * 
     * @param context The current DSpace application context.
     * @param root The root json node to extract data from.
     * @return A list of MetadataValueDTO containing all extracted basic information.
     */
    private List<MetadataValueDTO> extractBasicInfo(Context context, ReadContext root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();

        // Title extraction
        addMetadata(mdValues, Publication.TITLE_FIELD,
            getFirst(root, "$.title"), null, CF_UNSET, false);
        // Language extraction
        addMetadata(mdValues, Publication.LANGUAGE_FIELD,
            mapLanguage(getFirst(root, "$.language")), null, CF_UNSET, false
        );
        // Abstract extraction
        addMetadata(mdValues, Publication.ABSTRACT_FIELD,
            getFirst(root, "$.abstract"), null, CF_UNSET, false);
        // Date issued extraction
        addMetadata(mdValues, Publication.DATE_ISSUED_FIELD,
            convertDateIssued(root.read("$.issued.date-parts[0]")), null, CF_UNSET, false);
        // Keywords extraction
        addAllMetadata(mdValues, Publication.KEYWORD_FIELD,
            root.read("$.subject"), null, CF_UNSET, false);
        return mdValues;
    }

    /**
     * Extract book-chapter information from the root json node.
     * In this case we extract host document information.
     * 
     * @param context The current DSpace application context.
     * @param root The root json node to extract data from.
     * @return A list of MetadataValueDTO containing all extracted book-chapter information.
     */
    private List<MetadataValueDTO> extractBookChapterInfo(Context context, ReadContext root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();
        mdValues.addAll(extractHostInfo(context, root));
        return mdValues;
    }

    /**
     * Extract book information from the root json node.
     * In this case we extract a potential ISBN and container title.
     * 
     * @param context The current DSpace application context.
     * @param root The root json node to extract data from.
     * @return A list of MetadataValueDTO containing all extracted book information.
     */
    private List<MetadataValueDTO> extractBookInfo(Context context, ReadContext root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();
        addMetadata(mdValues, Publication.IDENTIFIER_ISBN_FIELD,
            getFirst(root, "$.ISBN"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.COLLECTION_NAME_FIELD,
            getFirst(root, "$.container-title"), null, CF_UNSET, false);
        return mdValues;
    }

    /**
     * Extract article information from the root json node.
     * In this case we extract journal information.
     * 
     * @param context The current DSpace application context.
     * @param root The root json node to extract data from.
     * @return A list of MetadataValueDTO containing all extracted article information.
     */
    private List<MetadataValueDTO> extractArticleInfo(Context context, ReadContext root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();
        mdValues.addAll(extractJournalInfo(context, root));
        return mdValues;
    }

    /**
     * Extract speech information from the root json node.
     * In this case we extract basic conference information.
     * If an ISSN is present, we also extract journal information.
     * If an ISBN is present, we also extract host document information.
     * 
     * @param context The current DSpace application context.
     * @param root The root json node to extract data from.
     * @return A list of MetadataValueDTO containing all extracted speech information.
     */
    private List<MetadataValueDTO> extractSpeechInfo(Context context, ReadContext root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();
        // Conference name extraction.
        addMetadata(mdValues, Publication.CONFERENCE_NAME_FIELD,
            getFirst(root, "$.assertion[?(@.name == 'conference_name')].value"), null, CF_UNSET, false);

        // Conference location extraction.
        String city = getFirst(root, "$.assertion[?(@.name == 'conference_city')].value");
        String country = getFirst(root, "$.assertion[?(@.name == 'conference_country')].value");

        String joined = Stream.of(city, country)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.joining(", "));

        addMetadata(mdValues, Publication.CONFERENCE_LOCATION_FIELD, joined, null, CF_UNSET, false);

        // Conference start and end date extraction.
        String startDate = DateUtils.convertDateString(
            getFirst(root, "$.assertion[?(@.name == 'conference_start_date')].value"),
            DateUtils.DSPACE_FORMAT
        );
        String endDate = DateUtils.convertDateString(
            getFirst(root, "$.assertion[?(@.name == 'conference_end_date')].value"),
            DateUtils.DSPACE_FORMAT
        );
        addMetadata(mdValues, Publication.CONFERENCE_START_DATE_FIELD,
                startDate, null, CF_UNSET, false
        );
        addMetadata(mdValues, Publication.CONFERENCE_END_DATE_FIELD,
                endDate, null, CF_UNSET, false
        );

        // Handle case where speech is published in serial or book.
        if (StringUtils.isNotEmpty(getFirst(root, "$.ISSN"))) {
            addMetadata(mdValues, Publication.SPEECH_STATUS_FIELD,
                SpeechPublication.STATUS_PUBLISHED_SERIAL, null, CF_UNSET, false);
            mdValues.addAll(extractJournalInfo(context, root));
        } else if (StringUtils.isNotEmpty(getFirst(root, "$.ISBN"))) {
            addMetadata(mdValues, Publication.SPEECH_STATUS_FIELD,
                SpeechPublication.STATUS_PUBLISHED_BOOK, null, CF_UNSET, false);
            mdValues.addAll(extractHostInfo(context, root));
        }
        return mdValues;
    }

    /**
     * Extract journal information from the root json node.
     * In this case we extract extract journal information.
     * 
     * @param context The current DSpace application context.
     * @param root The root json node to extract data from.
     * @return A list of MetadataValueDTO containing all extracted article information.
     */
    private List<MetadataValueDTO> extractJournalInfo(Context context, ReadContext root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();
        // Volume, issue && pages.
        addMetadata(mdValues, Publication.JOURNAL_VOLUME_FIELD,
            getFirst(root, "$.volume"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.JOURNAL_ISSUE_FIELD,
            getFirst(root, "$.journal-issue.issue"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.JOURNAL_PAGES_FIELD,
            getFirst(root, "$.page"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.JOURNAL_DATE_ISSUED_FIELD,
            getFirst(root, "$.issued.date-parts[0][0]"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.PUBLICATION_STATUS_FIELD,
            Publication.STATUS_PUBLISHED, null, CF_UNSET, false);

        String issn = getFirst(root, "$.issn-type[?(@.type == 'print')].value");
        String eissn = getFirst(root, "$.issn-type[?(@.type == 'electronic')].value");

        Journal journal = (issn != null || eissn != null)
            ? journalService.findByIdentifiers(context, issn, eissn)
            : null;
        if (journal != null) {
            String authority = journal.getID().toString();
            addMetadata(mdValues, Publication.JOURNAL_ISSN_FIELD,
                journal.getIdentifier(Journal.ISSN_IDENTIFIER), authority, CF_ACCEPTED, false);
            addMetadata(mdValues, Publication.JOURNAL_EISSN_FIELD,
                journal.getIdentifier(Journal.EISSN_IDENTIFIER), authority, CF_ACCEPTED, false);
            addMetadata(mdValues, Publication.JOURNAL_TITLE_FIELD,
                journal.getTitle(), authority, CF_ACCEPTED, false);
            addMetadata(mdValues, Publication.EDITOR_NAME_FIELD,
                journal.getPublisher(), authority, CF_ACCEPTED, false);
            addMetadata(mdValues, Publication.EDITOR_LOCATION_FIELD,
                journal.getPublisherLocation(), authority, CF_ACCEPTED, false);
            addMetadata(mdValues, Publication.JOURNAL_PEER_REVIEWED_FIELD,
                journal.getPeerReviewed(), authority, CF_ACCEPTED, false);
            return mdValues;
        }

        addMetadata(mdValues, Publication.JOURNAL_ISSN_FIELD,
            issn, null, CF_UNSET, false);
        addMetadata(mdValues, Publication.JOURNAL_EISSN_FIELD,
            eissn, null, CF_UNSET, false);
        addMetadata(mdValues, Publication.JOURNAL_TITLE_FIELD,
            getFirst(root, "$.container-title"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.EDITOR_NAME_FIELD,
            getFirst(root, "$.publisher"), null, CF_UNSET, false);

        return mdValues;
    }

    /**
     * Extract host document information from the root json node.
     * 
     * @param context The current DSpace application context.
     * @param root The root json node to extract data from.
     * @return A list of MetadataValueDTO containing all extracted host document information.
     */
    private List<MetadataValueDTO> extractHostInfo(Context context, ReadContext root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();

        addMetadata(mdValues, Publication.PUBLICATION_STATUS_FIELD,
            Publication.STATUS_PUBLISHED, null, CF_UNSET, false);

        addMetadata(mdValues, Publication.HOST_DOCUMENT_TITLE_FIELD,
            getFirst(root, "$.container-title"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.HOST_DOCUMENT_ISBN_FIELD,
            getFirst(root, "$.ISBN"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.HOST_DOCUMENT_PAGES_FIELD,
            getFirst(root, "$.page"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.HOST_DOCUMENT_YEAR_FIELD,
            getFirst(root, "$.issued.date-parts[0][0]"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.EDITOR_NAME_FIELD,
            getFirst(root, "$.publisher"), null, CF_UNSET, false);
        return mdValues;
    }

    /**
     * Clean a given orcid string to get rid of the orcid URL prefix.
     * 
     * @param orcidURL The orcidURL to clean.
     */
    private String extractOrcid(String orcidURL) {
        // Get rid of the url present before the actual ORCID.
        return orcidURL != null ? orcidURL.replaceFirst("https?://orcid\\.org/", "") : null;
    }

    /**
     * Map the found type from crossRef to a valid Publication type.
     * 
     * @param crossRefType The type coming from crossRef.
     * @return The mapped type string. null if none found.
     */
    private String mapMainType(String crossRefType) {
        if (StringUtils.isEmpty(crossRefType)) {
            return null;
        }
        return switch (crossRefType) {
            case "monograph", "book" -> BookPublication.DOCUMENT_TYPE;
            case "book-chapter" -> BookChapterPublication.DOCUMENT_TYPE;
            case "journal-article" -> ArticlePublication.DOCUMENT_TYPE;
            case "proceedings-article", "posted-content" -> SpeechPublication.DOCUMENT_TYPE;
            case "report" -> ReportPublication.DOCUMENT_TYPE;
            default -> null;
        };
    }

    /**
     * Map the given crossRef language to a supported Publication language (iso639 -> iso639_2).
     * 
     * @param crossRefLanguage The language code coming from crossRef.
     * @return The mapped language string in 639_2 format. Return 'und' if no mapping found.
     */
    private String mapLanguage(String crossRefLanguage) {
        if (StringUtils.isEmpty(crossRefLanguage)) {
            return null;
        }
        return switch (crossRefLanguage) {
            case "nl" -> Publication.LANGUAGE_DUTCH;
            case "en" -> Publication.LANGUAGE_ENGLISH;
            case "fr" -> Publication.LANGUAGE_FRENCH;
            case "it" -> Publication.LANGUAGE_ITALIAN;
            case "de" -> Publication.LANGUAGE_GERMAN;
            case "el" -> Publication.LANGUAGE_GREEK;
            case "la" -> Publication.LANGUAGE_LATIN;
            case "pl" -> Publication.LANGUAGE_POLISH;
            case "pt" -> Publication.LANGUAGE_PORTUGUESE;
            case "ru" -> Publication.LANGUAGE_RUSSIAN;
            case "es" -> Publication.LANGUAGE_SPANISH;
            default -> Publication.LANGUAGE_OTHER;
        };
    }

    /**
     * Parse a date string from the given date node.
     * The date node as a format of ['year', 'month'(optional), 'day'(optional)].
     * We want to build a 'DSpace valid' date from this array.
     * 
     * @param dateNode The date node containing the date information.
     * @return A parsed date string containing at least a year.
     */
    private String convertDateIssued(List<Integer> dateNode) {
        if (dateNode == null || dateNode.isEmpty()) {
            return null;
        }

        int size = dateNode.size();
        String year = dateNode.get(0).toString(); // Get year (mandatory).

        if (size == 1) {
            return year;
        }

        int month = dateNode.get(1);
        if (size == 2) {
            return "%s-%02d".formatted(year, month);
        }

        int day = dateNode.get(2);
        return "%s-%02d-%02d".formatted(year, month, day);
    }

    // GETTERS AND SETTERS
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
