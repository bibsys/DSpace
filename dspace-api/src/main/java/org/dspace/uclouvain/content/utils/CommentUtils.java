/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.utils;

import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.core.Context;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.uclouvain.content.LegacyComment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class CommentUtils {

    private static final Logger log = LogManager.getLogger(CommentUtils.class);

    // Makes sure that utility classes (classes that contain only static methods or fields in their API) do not have
    // a public constructor.
    protected CommentUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * This method allows getting comments stored into a FedoraCommons legacy COMMENT bitstream.
     *
     * @param context the dspace application context
     * @param bitstream the COMMENT bitstream to analyze
     * @return the list of loaded {@link org.dspace.uclouvain.content.LegacyComment}
     */
    public static List<LegacyComment> loadLegacyComments(Context context, Bitstream bitstream) {
        BitstreamStorageService storageService = StorageServiceFactory.getInstance().getBitstreamStorageService();
        List<LegacyComment> comments = new ArrayList<>();

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(storageService.retrieve(context, bitstream)));
            NodeList nodeList = document.getElementsByTagName("comment");
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                comments.add(buildComment(element));
            }
        } catch (Exception e) {
            log.error("Unable to load legacy comment :: " + e.getMessage(), e);
        }
        return comments;
    }

    private static LegacyComment buildComment(Element xmlElt) {
        LegacyComment comment = new LegacyComment();
        comment.setWriter(xmlElt.hasAttribute("writer") ? xmlElt.getAttribute("writer") : "unknown");
        comment.setContent(xmlElt.getTextContent());
        comment.setCreated(xmlElt.hasAttribute("timestamp") ? xmlElt.getAttribute("timestamp") : "1970-01-01");
        return comment;
    }
}
