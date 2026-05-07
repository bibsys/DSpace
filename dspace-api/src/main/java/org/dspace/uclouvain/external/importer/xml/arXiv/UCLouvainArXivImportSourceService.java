/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.importer.xml.arXiv;

import static org.dspace.content.authority.Choices.CF_UNSET;

import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.importer.external.liveimportclient.service.LiveImportClient;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.external.importer.xml.UCLouvainXMLImportSourceService;
import org.dspace.web.ContextUtil;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.jdom2.xpath.XPathExpression;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to extract a list of MetadataValueDTO from Pubmed for a given ArXiv identifier.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class UCLouvainArXivImportSourceService extends UCLouvainXMLImportSourceService {
    private static final Logger logger = LogManager.getLogger(UCLouvainArXivImportSourceService.class);
    private static final String ARXIV_URL_PREFIX = "http://arxiv.org/abs/";
    private static final String ARXIV_PREFIX = "arxiv:";

    @Autowired
    private LiveImportClient liveImportClient;

    private String url;

    @Override
    protected List<Namespace> getNamespaces() {
        return Arrays.asList(
            Namespace.getNamespace("ns", "http://www.w3.org/2005/Atom"),
            Namespace.getNamespace("arxiv", "http://arxiv.org/schemas/atom")
        );
    }

    @Override
    public List<MetadataValueDTO> getMetadataList(String query) {
        try {
            Context context = ContextUtil.obtainCurrentRequestContext();
            String rawResponse = fetchData(extractID(query));
            Element parsedXml = parseXmlResponse(rawResponse);
            return generateMetadataList(context, parsedXml);
        } catch (URISyntaxException e) {
            logger.error("Could not build ArXiv request URL.", e);
            return List.of();
        }
    };

    private String extractID(String query) {
        if (StringUtils.isNotBlank(query)) {
            query = query.trim();
            if (query.startsWith(ARXIV_URL_PREFIX)) {
                query = query.substring(ARXIV_URL_PREFIX.length());
            } else if (query.toLowerCase().startsWith(ARXIV_PREFIX)) {
                query = query.substring(ARXIV_PREFIX.length());
            }
        }
        return query;
    }

    private String fetchData(String query) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(url);
        uriBuilder.addParameter("id_list", query);
        Map<String, Map<String, String>> params = new HashMap<>();
        return liveImportClient.executeHttpGetRequest(2000, uriBuilder.toString(), params);
    }

    private Element parseXmlResponse(String xmlResponse) {
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new StringReader(xmlResponse));
            Element root = document.getRootElement();

            XPathExpression<Element> xpath = buildXpath("ns:entry", getNamespaces());

            List<Element> recordsList = xpath.evaluate(root);
            return recordsList != null ? recordsList.get(0) : null;
        } catch (JDOMException | IOException e) {
            return null;
        }
    }

    // METADATA EXTRACTION =============================================================================================

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
        // Author names
        List<String> authors = getAllText(rootXml, "ns:author/ns:name");
        if (authors.size() > authorLimit) {
            authors = authors.subList(0, authorLimit);
            addMetadata(metadataList, Publication.AUTHOR_ETAL_FIELD, "true", null, CF_UNSET, false);
            addAllMetadata(metadataList, Publication.AUTHOR_NAME_FIELD, authors, null, CF_UNSET, false);
        } else {
            addAllMetadata(metadataList, Publication.AUTHOR_NAME_FIELD, authors, null, CF_UNSET, false);
        }
        // Title
        addMetadata(metadataList, Publication.TITLE_FIELD,
            getFirstText(rootXml, "ns:title"), null, CF_UNSET, false);
        // Abstract (summary)
        addMetadata(metadataList, Publication.ABSTRACT_FIELD,
            getFirstText(rootXml, "ns:summary"), null, CF_UNSET, false);
        // DOI
        addMetadata(metadataList, Publication.DOI_IDENTIFIER_FIELD,
            getFirstText(rootXml, "arxiv:doi"), null, CF_UNSET, false);
        // Date issued (published)
        addMetadata(metadataList, Publication.DATE_ISSUED_FIELD,
            convertToDSpaceDate(getFirstText(rootXml, "ns:published")), null, CF_UNSET, false);
        // Journal info (title)
        addMetadata(metadataList, Publication.JOURNAL_TITLE_FIELD,
            getFirstText(rootXml, "arxiv:journal_ref"), null, CF_UNSET, false);
        // Keyword (term)
        addMetadata(metadataList, Publication.KEYWORD_FIELD,
            getFirstText(rootXml, "ns:category/@term"), null, CF_UNSET, false);

        return metadataList;
    }

    /**
     * Convert a date from arXiv (ISO 8601) to a valid dspace date.
     * @param date The date string to convert.
     * @return A DSpace valid date string composed of 'year-month-day'.
     */
    private String convertToDSpaceDate(String date) {
        if (StringUtils.isEmpty(date)) {
            return null;
        }
        String finalDate = Instant.parse(date)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()
            .toString();
        return finalDate;
    }

    // GETTERS AND SETTERS =============================================================================================

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}
