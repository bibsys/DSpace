/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.importer.xml.pubmed;

import static org.dspace.content.authority.Choices.CF_ACCEPTED;
import static org.dspace.content.authority.Choices.CF_UNSET;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.profile.ResearcherProfile;
import org.dspace.uclouvain.core.model.Journal;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.external.importer.xml.UCLouvainXMLImportSourceService;
import org.dspace.uclouvain.services.JournalService;
import org.dspace.uclouvain.services.UCLouvainProfileService;
import org.dspace.web.ContextUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.InputSource;

/**
 * Service to extract a list of MetadataValueDTO from Pubmed for a given
 * PubmedId.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainPubmedImportSourceService extends UCLouvainXMLImportSourceService {

    private static final Logger logger = LogManager.getLogger(UCLouvainPubmedImportSourceService.class);
    private static final String ORCID_IDENTIFIER = "ORCID";
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);

    @Autowired
    private LiveImportClient liveImportClient;
    @Autowired
    private UCLouvainProfileService uclouvainProfileService;
    @Autowired
    private JournalService journalService;

    private String urlFetch;
    private String urlSearch;

    public List<MetadataValueDTO> getMetadataList(String query) {
        try {
            Context context = ContextUtil.obtainCurrentRequestContext();
            String rawResponse = fetchData(query);
            Element parsedXml = parseXmlResponse(rawResponse);
            return generateMetadataList(context, parsedXml);
        } catch (URISyntaxException e) {
            logger.error("Could not build Pubmed request URL.", e);
            return List.of();
        }
    };

    private String fetchData(String query) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(urlFetch);
        uriBuilder.addParameter(query, query);
        uriBuilder.addParameter("db", "pubmed");
        uriBuilder.addParameter("retmode", "xml");
        uriBuilder.addParameter("id", query);
        Map<String, Map<String, String>> params = new HashMap<>();
        return liveImportClient.executeHttpGetRequest(2000, uriBuilder.toString(), params);
    }

    private Element parseXmlResponse(String xmlResponse) {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            // Disallow external entities & entity expansion to protect against XXE attacks
            // (NOTE: We receive errors if we disable all DTDs for PubMed, so this is the best we can do)
            saxBuilder.setFeature("http://xml.org/sax/features/external-general-entities", false);
            saxBuilder.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            saxBuilder.setExpandEntities(false);
            saxBuilder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));

            Document document = saxBuilder.build(new StringReader(xmlResponse));
            Element root = document.getRootElement();

            XPathExpression<Element> xpath = buildXpath("//PubmedArticle");

            List<Element> recordsList = xpath.evaluate(root);
            return recordsList != null ? recordsList.get(0) : null;
        } catch (JDOMException | IOException e) {
            return null;
        }
    }

    // METADATA EXTRACTION
    // =============================================================================================

    /**
     * Generate a list of all metadata values extracted from the external source.
     * 
     * @param context The current Dspace application context.
     * @param rootXml The root document to extract data from.
     * @return A list of all extracted MetadataValueDTO.
     */
    private List<MetadataValueDTO> generateMetadataList(Context context, Element rootXml) {
        List<MetadataValueDTO> metadataList = new ArrayList<>();
        if (rootXml == null) {
            return metadataList;
        }
        metadataList.addAll(extractAuthors(context, rootXml));
        metadataList.addAll(extractBasicInfo(context, rootXml));
        metadataList.addAll(extractJournalInfo(context, rootXml));
        return metadataList;
    }

    /**
     * Import all possible authors from the source.
     * If an OrcidID is present for an author, try to find a matching person profile in DSpace and use it as authority.
     * 
     * @param context The current DSpace context.
     * @param root    The root XML object coming from the source.
     * @return A list of extracted MetadataValueDTO.
     */
    private List<MetadataValueDTO> extractAuthors(Context context, Element root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();
        List<Element> authors = buildXpath("//AuthorList/Author").evaluate(root);
        for (Element author : authors) {

            Element identifier = author.getChild("Identifier");
            String orcid = (identifier != null && ORCID_IDENTIFIER.equals(identifier.getAttributeValue("Source")))
                ? identifier.getTextTrim()
                : null;

            // Try to enrich metadata values by finding a matching profile.
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
            } else {
                // We don't find any matching profile. Just create author name, orcid and affiliation metadata based
                // on source input (email & fgs are not relevant in this case)
                // TODO : Try to extract institution from affiliation ???
                String authorName = "%s, %s".formatted(
                    author.getChildTextTrim("LastName"),
                    author.getChildTextTrim("ForeName")
                );
                addMetadata(mdValues, Publication.AUTHOR_NAME_FIELD, authorName, null, CF_UNSET, true);
                addMetadata(mdValues, Publication.AUTHOR_ORCID_FIELD, orcid, null, CF_UNSET, true);
                addMetadata(mdValues, Publication.AUTHOR_INSTITUTION_FIELD, null, null, CF_UNSET, true);
                addMetadata(mdValues, Publication.AUTHOR_FGS_FIELD, null, null, CF_UNSET, true);
                addMetadata(mdValues, Publication.AUTHOR_EMAIL_FIELD, null, null, CF_UNSET, true);
            }
        }
        return mdValues;
    }

    /**
     * Import all possible basic information from the source.
     * 
     * @param context The current DSpace context.
     * @param root    The root XML object coming from the source.
     * @return A list of extracted MetadataValueDTO.
     */
    private List<MetadataValueDTO> extractBasicInfo(Context context, Element root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();
        addMetadata(mdValues, Publication.TITLE_FIELD,
            getFirstText(root, "//ArticleTitle"), null, CF_UNSET, false);
        // TODO: Multiple abstract can exist see how to extract them.
        addAllMetadata(mdValues, Publication.ABSTRACT_FIELD,
            getAllText(root, "//AbstractText"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.DATE_ISSUED_FIELD,
            parsePubmedDate(buildXpath("//PubDate").evaluateFirst(root)), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.LANGUAGE_FIELD,
            getFirstText(root, "//Language"), null, CF_UNSET, false);
        addAllMetadata(mdValues, Publication.KEYWORD_FIELD,
            getAllText(root, "//Keyword"), null, CF_UNSET, false);
        addAllMetadata(mdValues, Publication.MESH_KEYWORD_FIELD,
            getAllText(root, "//MeshHeading/DescriptorName"),null, CF_UNSET, false);
        return mdValues;
    }

    /**
     * Import all possible journal information from the source.
     * If an issn or e-issn is present, try to find a matching journal in DSpace and
     * use it as authority.
     * 
     * @param context The current DSpace context.
     * @param root    The root XML object coming from the source.
     * @return A list of extracted MetadataValueDTO.
     */
    private List<MetadataValueDTO> extractJournalInfo(Context context, Element root) {
        List<MetadataValueDTO> mdValues = new ArrayList<>();

        // Handle publication status
        String publicationStatus = mapPublicationStatus(getFirstText(root, "//PubmedData/PublicationStatus"));
        addMetadata(mdValues, Publication.PUBLICATION_STATUS_FIELD,
            publicationStatus, null, CF_UNSET, false);
        // Extract volume, issue and pagination (+year?).
        addMetadata(mdValues, Publication.JOURNAL_VOLUME_FIELD,
            getFirstText(root, "//Journal/JournalIssue/Volume"), null, CF_UNSET, false);
        addMetadata(mdValues, Publication.JOURNAL_ISSUE_FIELD,
            getFirstText(root, "//Journal/JournalIssue/Issue"), null, CF_UNSET, false);
        String journalPages = extractJournalPages(buildXpath("//Article/Pagination").evaluateFirst(root));
        addMetadata(mdValues, Publication.JOURNAL_PAGES_FIELD,
            journalPages, null, CF_UNSET, false);
        addMetadata(mdValues, Publication.JOURNAL_DATE_ISSUED_FIELD,
            getFirstText(root, "//Journal/JournalIssue/PubDate/Year"), null, CF_UNSET, false);

        String issn = getFirstText(root, "//MedlineJournalInfo/ISSNLinking");
        String eissn = getFirstText(root, "//Journal/ISSN[@IssnType='Electronic']");
        Journal journal = (issn != null || eissn != null)
            ? journalService.findByIdentifiers(context, issn, eissn)
            : null;
        if (journal != null) {
            String authority = journal.getID().toString();
            int confidence = CF_ACCEPTED;
            addMetadata(mdValues, Publication.JOURNAL_ISSN_FIELD,
                journal.getIdentifier(Journal.ISSN_IDENTIFIER), authority, confidence, false);
            addMetadata(
                mdValues, Publication.JOURNAL_EISSN_FIELD,
                journal.getIdentifier(Journal.EISSN_IDENTIFIER), authority, confidence, false);
            addMetadata(mdValues, Publication.JOURNAL_TITLE_FIELD,
                journal.getTitle(), authority, confidence, false);
            addMetadata(mdValues, Publication.EDITOR_NAME_FIELD,
                journal.getPublisher(), authority, confidence, false);
            addMetadata(mdValues, Publication.EDITOR_LOCATION_FIELD,
                journal.getPublisherLocation(), authority, confidence, false);
            addMetadata(mdValues, Publication.JOURNAL_PEER_REVIEWED_FIELD,
                journal.isPeerReviewedString(), authority, confidence, false);
        } else {
            addMetadata(mdValues, Publication.JOURNAL_ISSN_FIELD, issn, null, CF_UNSET, false);
            addMetadata(mdValues, Publication.JOURNAL_EISSN_FIELD, eissn, null, CF_UNSET, false);
            addMetadata(mdValues, Publication.JOURNAL_TITLE_FIELD,
                getFirstText(root, "//Title"), null, CF_UNSET, false);
        }
        return mdValues;
    }

    // Private methods specific to Pubmed import.

    /**
     * Parse a date found in Pubmed XML to a common DSpace date.
     * 
     * @param pubDateElem The XML element containing the date information.
     * @return A DSpace formatted date string.
     */
    private String parsePubmedDate(Element pubDateElem) {
        if (pubDateElem == null) {
            return null;
        }
        // At least the year is required, so if none: exit.
        String year = pubDateElem.getChildTextTrim("Year");
        if (StringUtils.isBlank(year)) {
            return null;
        }
        // If no month is found, just return the year.
        String month = pubDateElem.getChildTextTrim("Month");
        if (StringUtils.isBlank(month)) {
            return year;
        }
        String isoMonth = tryParseMonth(month);
        if (isoMonth == null) {
            return year;
        }
        String iso = "%s-%s".formatted(year, isoMonth);
        // Try to add a day if possible
        String day = pubDateElem.getChildTextTrim("Day");
        if (StringUtils.isNumeric(day)) {
            return "%s-%02d".formatted(iso, Integer.parseInt(day));
        }
        return iso;
    }

    private String tryParseMonth(String monthName) {
        try {
            int monthVal = MONTH_FORMATTER.parse(monthName).get(ChronoField.MONTH_OF_YEAR);
            return "%02d".formatted(monthVal);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Map any pubmed 'publication status' to our own vocabulary.
     * 
     * @param publicationStatus The publication status string.
     * @return The mapped publication string, if no mapping found, return null.
     */
    private String mapPublicationStatus(String publicationStatus) {
        if (StringUtils.isNotEmpty(publicationStatus)) {
            return switch (publicationStatus) {
                case "ppublish", "received", "epublish" -> Publication.STATUS_PUBLISHED;
                case "aheadofprint" -> Publication.STATUS_INPRESS;
                default -> null;
            };
        }
        return null;
    }

    /**
     * Extract journal pages information from a given 'Pagination' XML element.
     * 
     * @param pagination The pagination XML element to extract information from.
     * @return Return a pagination range.
     */
    private String extractJournalPages(Element pagination) {
        if (pagination == null) {
            return null;
        }
        String start = pagination.getChildTextTrim("StartPage");
        String end = pagination.getChildTextTrim("EndPage");
        // we found both data & it's not the same value
        if (StringUtils.isNotEmpty(start) && StringUtils.isNotEmpty(end) && !end.equals(start)) {
            return "%s-%s".formatted(start, end);
        }
        // we only found the start
        if (StringUtils.isNotEmpty(start)) {
            return start;
        }
        // fallback
        return pagination.getChildTextTrim("MedlinePgn");
    }

    // GETTERS AND SETTERS
    public String getUrlFetch() {
        return urlFetch;
    }

    public void setUrlFetch(String urlFetch) {
        this.urlFetch = urlFetch;
    }

    public String getUrlSearch() {
        return urlSearch;
    }

    public void setUrlSearch(String urlSearch) {
        this.urlSearch = urlSearch;
    }
}
