/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.uclouvain.content.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Implementation of a comment service if the comments are store into an item bitstream.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class XMLBitstreamCommentServiceImpl implements CommentService {

    private static final Logger log = LogManager.getLogger(XMLBitstreamCommentServiceImpl.class);
    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",  // DateTimeFormatter.ISO_FORMAT
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss",
        "MM-dd-yyyy HH:mm:ss",
        "yyyy-MM-dd"
    };

    ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();
    ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance().getBitstreamFormatService();
    BitstreamStorageService bitstreamStorageService = StorageServiceFactory.getInstance().getBitstreamStorageService();
    EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();

    // INTERFACE FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public List<Comment> getComments(Context context, Item item) throws Exception {
        Bitstream commentBitstream = getCommentBitstream(item);
        return (commentBitstream != null)
            ? extractComments(context, item, commentBitstream)
                .stream()
                .sorted(Comparator.comparing(Comment::getCreationDate))
                .collect(Collectors.toList())
            : Collections.EMPTY_LIST;
    }

    @Override
    public Comment getComment(Context context, Item item, String commentID) throws Exception {
        return getComments(context, item).stream().filter(c -> c.getId().equals(commentID)).findFirst().orElse(null);
    }

    private Comment addComment(Context context, Item item, String authorName, UUID authorID, String content)
            throws Exception {
        Comment newComment = new Comment();
        newComment.setAuthorName(authorName);
        newComment.setAuthorAuthority(authorID);
        newComment.setCreationDate(Date.from(Instant.now()));
        newComment.setModifiedDate(newComment.getCreationDate());
        newComment.setId(newComment.getCreationDate().toInstant().toString());
        newComment.setContent(content);

        List<Comment> comments = getComments(context, item);
        if (comments == Collections.EMPTY_LIST) {
            comments = new ArrayList<>();
        }
        comments.add(newComment);
        saveComments(context, item, comments);
        return newComment;
    }

    @Override
    public Comment addComment(Context context, Item item, EPerson person, String content) throws Exception {
        return addComment(context, item, person.getFullName(), person.getID(), content);
    }

    @Override
    public Comment addComment(Context context, Item item, String authorName, String content) throws Exception {
        return addComment(context, item, authorName, null, content);
    }

    @Override
    public Comment addSystemComment(Context context, Item item, String content) throws Exception {
        return addComment(context, item, "system", content);
    }

    @Override
    public void deleteComment(Context context, Item item, String commentID) throws Exception {
        List<Comment> comments = getComments(context, item);
        comments.removeIf(c -> c.getId().equals(commentID));
        if (comments.isEmpty()) {
            removeCommentBitstream(context, item);
        } else {
            saveComments(context, item, comments);
        }

    }

    @Override
    public void deleteAllComment(Context context, Item item) throws SQLException, AuthorizeException, IOException {
        removeCommentBitstream(context, item);
    }

    @Override
    public void updateComment(Context context, Item item, String commentID, String commentContent) throws Exception {
        List<Comment> comments = getComments(context, item);
        Optional<Comment> commentToUpdate = comments.stream().filter(c -> c.getId().equals(commentID)).findFirst();
        if (commentToUpdate.isPresent()) {
            commentToUpdate.get().setContent(commentContent);
            commentToUpdate.get().setModifiedDate(Date.from(Instant.now()));
        }
        saveComments(context, item, comments);
    }

    // PRIVATE FUNCTION ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Get the bitstream where comments are stored
     *
     * @param item the {@link org.dspace.content.Item} containing the 'comment' bitstream
     * @return the {@link org.dspace.content.Bitstream} containing comments about this item. Return null if not found
     * @throws SQLException raised when any database exception occurred.
     */
    private Bitstream getCommentBitstream(Item item) throws SQLException {
        String bundleCommentProperties = configService.getProperty("comments.bitstream.bundle.location", "COMMENT");
        String bitstreamNameCommentProperties = configService.getProperty("comments.bitstream.name.location");

        if (StringUtils.isNotBlank(bitstreamNameCommentProperties)) {
            Bitstream bitstream = bitstreamService
                .getBitstreamByName(item, bundleCommentProperties, bitstreamNameCommentProperties);
            if (bitstream != null) {
                return bitstream;
            }
        }
        return itemService
            .getBundles(item, bundleCommentProperties)
            .stream()
            .map(Bundle::getBitstreams)
            .flatMap(List::stream)
            .findFirst()
            .orElse(null);
    }

    /**
     * Save comments as a bitstream into the Item
     * @param context the application {@link org.dspace.core.Context}
     * @param item the {@link org.dspace.content.Item} related to comments
     * @param comments the list of {@link org.dspace.uclouvain.content.Comment} to save
     * @throws Exception if any exception occurred during the process
     */
    private void saveComments(Context context, Item item, List<Comment> comments) throws Exception {
        InputStream commentsXML = convertXMLDocumentToInputStream(buildComments(comments));
        Bitstream commentBitstream = getCommentBitstream(item);

        if (commentBitstream == null) {
            commentBitstream = createCommentBitstream(context, item, commentsXML);
            log.info("Comments saved into newly created `" + commentBitstream.getID() + "` bitstream");
        } else {
            bitstreamStorageService.store(context, commentBitstream, commentsXML);
            log.info("Comments saved into existing `" + commentBitstream.getID() + "` bitstream");
        }
    }

    /**
     * Create the bitstream where the comments will be stored
     *    This function must not be called if the `comment` bitstream already exists
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the {@link org.dspace.content.Item} related to comments
     * @param bitstreamContent the bitstream content to store.
     * @return The created bitstream
     * @throws SQLException raised when any database exception occurred.
     * @throws IOException raised if bitstream content cannot be read.
     */
    private Bitstream createCommentBitstream(Context context, Item item, InputStream bitstreamContent)
            throws SQLException, IOException {
        String bundleCommentProperties = configService.getProperty("comments.bitstream.bundle.location", "COMMENT");
        String bitstreamNameProperties = configService.getProperty("comments.bitstream.name.location", "comments");

        context.turnOffAuthorisationSystem();
        Bitstream commentBitstream = null;
        try {
            List<Bundle> bundles = itemService.getBundles(item, bundleCommentProperties);
            Bundle commentBundle = (bundles.isEmpty())
                    ? bundleService.create(context, item, bundleCommentProperties)
                    : bundles.get(0);
            commentBitstream = bitstreamService.create(context, commentBundle, bitstreamContent);
            commentBitstream.setName(context, bitstreamNameProperties);
            commentBitstream.setFormat(context, bitstreamFormatService.findByMIMEType(context, "text/xml"));
            bundleService.addBitstream(context, commentBundle, commentBitstream);

            bitstreamService.update(context, commentBitstream);
            bundleService.update(context, commentBundle);
            itemService.update(context, item);

            return commentBitstream;
        } catch (AuthorizeException ae) {
            // Should never happen because we turn off the authorization system.
        } finally {
            context.restoreAuthSystemState();
        }
        return commentBitstream;
    }

    /**
     * Extract comments from a bitstream
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the parent {@link org.dspace.content.Item}
     * @param bitstream the 'comment' bitstream
     * @return the list of {@link org.dspace.uclouvain.content.Comment} found in the bitstream
     * @throws Exception for any exception occurred during comment bitstream parsing
     */
    private List<Comment> extractComments(Context context, Item item, Bitstream bitstream) throws Exception {
        List<Comment> comments = new ArrayList<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(bitstreamStorageService.retrieve(context, bitstream)));
        NodeList nodeList = document.getElementsByTagName("comment");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            comments.add(buildComment(context, element, item));
        }
        return comments;
    }

    /**
     * Remove the bitstream where comments are stored.
     *
     * @param context the application {@link org.dspace.core.Context}
     * @param item the parent {@link org.dspace.content.Item}
     * @throws SQLException for any database exception
     * @throws AuthorizeException for any authorization exception (should not happen since we turn off the auth system)
     * @throws IOException for any disk access error
     */
    private void removeCommentBitstream(Context context, Item item)
            throws SQLException, AuthorizeException, IOException {
        Bitstream commentBitstream = getCommentBitstream(item);
        if (commentBitstream != null) {
            List<Bundle> bundles = commentBitstream.getBundles();
            Bundle bundle = bundles.get(0);
            context.turnOffAuthorisationSystem();
            bundleService.removeBitstream(context, bundle, commentBitstream);
            List<Bitstream> bitstreams = bundle.getBitstreams();
            // remove the bundle if it's now empty
            if (bitstreams.isEmpty()) {
                itemService.removeBundle(context, item, bundle);
                itemService.update(context, item);
            }
            bundleService.update(context, bundle);
            context.restoreAuthSystemState();
        }
    }


    // XML FUNCTIONS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    /**
     * Build '<comments>' XML document from a list of {@link org.dspace.uclouvain.content.Comment} object.
     *
     * @param comments the list of comment.
     * @return the XML document representing the comment list.
     * @throws ParserConfigurationException if any error occurred during XML document creation
     */
    private Document buildComments(List<Comment> comments) throws ParserConfigurationException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element rootElement = doc.createElement("comments");
        doc.appendChild(rootElement);
        comments.forEach(c -> buildXMLElement(doc, rootElement, c));
        return doc;
    }

    /**
     * Parse an XML 'comment' tag to a {@link org.dspace.uclouvain.content.Comment} object.
     *
     * @param context the application context
     * @param element the XML element representing the comment.
     * @param item the parent item
     * @return the corresponding {@link org.dspace.uclouvain.content.Comment}
     * @throws Exception for any parsing exception
     */
    private Comment buildComment(Context context, Element element, Item item) throws Exception {
        Comment comment = new Comment();
        comment.setParent(item);
        comment.setContent(element.getTextContent());

        // Parse owner
        //   comment owner could be found in multiple attribute.
        //    * Either the comment has an 'ownerAuthority' attribute and this attribute can be related to an existing
        //      ePerson. In this case, we can found owner name and authority from ePerson.
        //    * Either the comment has only an 'owner' attribute representing the owner UID in the legacy system. In
        //      this case, we can't find any authority
        EPerson author = null;
        if (element.hasAttribute("authorAuthority")) {
            author = ePersonService.find(context, UUID.fromString(element.getAttribute("authorAuthority")));
        }
        if (author != null) {
            comment.setAuthorAuthority(author.getID());
            comment.setAuthorName(author.getFullName());

        } else {
            String authorName = (element.hasAttribute("author"))
                ? element.getAttribute("author")
                : (element.hasAttribute("writer")) ? element.getAttribute("writer")  : null;
            comment.setAuthorName(authorName);
        }

        // Parse dates
        //  1) creation date is store either into 'timestamp', either into 'creationDate' attribute
        //  2) modified date could be store into 'lastModified' attribute
        //  We cannot know all date format used, We can only predict them and use commonly known formats
        String creationDateStr = element.hasAttribute("timestamp")
                ? element.getAttribute("timestamp")
                : element.hasAttribute("creationDate") ? element.getAttribute("creationDate") : "1970-01-01";
        Date timestamp = DateUtils.parseDate(creationDateStr, DATE_PATTERNS);
        if (timestamp == null) {
            log.warn("Unable to parse comment creation date :: " + creationDateStr);
            timestamp = new Date(0);
        }
        comment.setCreationDate(timestamp);
        comment.setModifiedDate(timestamp);

        if (element.hasAttribute("lastModified")) {
            timestamp = DateUtils.parseDate(element.getAttribute("lastModified"), DATE_PATTERNS);
            if (timestamp == null) {
                log.warn("Unable to parse comment modify date :: " + element.getAttribute("lastModified"));
                timestamp = new Date(0);
            }
            comment.setModifiedDate(timestamp);
        }

        // For XML stored comment, the comment ID is the string representation of the creation date.
        comment.setId(comment.getCreationDate().toInstant().toString());
        return comment;
    }

    /**
     * Build an XML '<comment>' element from a {@link org.dspace.uclouvain.content.Comment} object.
     *
     * @param doc the XML document
     * @param root the parent tag where the '<comment>' will be created.
     * @param comment the {@link org.dspace.uclouvain.content.Comment} to convert.
     */
    private void buildXMLElement(Document doc, Element root, Comment comment) {
        Element commentElement = doc.createElement("comment");
        commentElement.setTextContent(comment.getContent());
        // Comment author
        commentElement.setAttribute("author", comment.getAuthorName());
        if (comment.getAuthorAuthority() != null) {
            commentElement.setAttribute("authorAuthority", comment.getAuthorAuthority().toString());
        }
        // Dates
        commentElement.setAttribute("creationDate", comment.getCreationDate().toInstant().toString());
        if (comment.getModifiedDate() != null)  {
            commentElement.setAttribute("lastModified", comment.getModifiedDate().toInstant().toString());
        }
        root.appendChild(commentElement);
    }


    /**
     * Convert an XML document to indent and pretty-printed InputStream
     * @param doc the XML document to convert
     * @return the corresponding InputStream
     * @throws TransformerException for any error during transforming process
     */
    private InputStream convertXMLDocumentToInputStream(Document doc) throws TransformerException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(outputStream);
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

        return new ByteArrayInputStream(outputStream.toByteArray());
    }
}
