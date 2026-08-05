/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.snapshot;

import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.dspace.uclouvain.content.snapshot.element.FileSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.MetadataSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.SnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.SnapshotElementFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * This class allow to serialize/deserialize the content stored into the database
 * for an {@link ItemSnapshot}. We decided to store the content in database as a XML string (using TEXT field);
 * But it should be possible to store content into many other ways/formats....
 *
 * TODO This class should be an interface, and a concrete class defined into configuration could be implement it.
 *      Using this way, user could choose the format to use to store the ItemSnapshot content.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
@Component
public class ItemSnapshotContentSerializer implements Serializable {

    @Autowired
    SnapshotElementFactory elementFactory;

    /**
     * This method build the XML content to store into the `content` column into the database
     * @param snapshot the item snapshot to store
     * @return the string representation of the XML to store into database
     * @throws ParserConfigurationException If creation of the XML document failed
     * @throws TransformerException If XML document cannot be converted to string
     */
    public String serialize(ItemSnapshot snapshot) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

        // Root element
        //   The handle is purely informational (it is never read back by `deserialize`) and an item still into
        //   workflow doesn't have one yet, so only write the attribute when it is known.
        Element root = document.createElement("ItemSnapshot");
        root.setAttribute("uuid", snapshot.getItem().getID().toString());
        if (StringUtils.isNotBlank(snapshot.getItem().getHandle())) {
            root.setAttribute("handle", snapshot.getItem().getHandle());
        }
        document.appendChild(root);

        // 1. Process Metadata Fields
        List<MetadataSnapshotElement> mdElements = snapshot.getSnapshotElementsOfType(MetadataSnapshotElement.class);
        if (!mdElements.isEmpty()) {
            Element metadataTag = document.createElement("Metadata");
            for (MetadataSnapshotElement element : mdElements) {
                Element fieldTag = document.createElement("MetadataField");
                fieldTag.setAttribute("path", element.getPath());
                fieldTag.setTextContent(element.getValue());
                metadataTag.appendChild(fieldTag);
            }
            root.appendChild(metadataTag);
        }

        // 2. Process File Fields
        List<FileSnapshotElement> fileElements = snapshot.getSnapshotElementsOfType(FileSnapshotElement.class);
        if (!fileElements.isEmpty()) {
            Element filesTag = document.createElement("Files");
            for (FileSnapshotElement element : fileElements) {
                Element fileTag = document.createElement("File");
                fileTag.setAttribute("uuid", element.getUUID().toString());
                fileTag.setAttribute("name", element.getFilename());
                fileTag.setAttribute("checksum", element.getChecksum());
                fileTag.setAttribute("access", element.getAccess());
                filesTag.appendChild(fileTag);
            }
            root.appendChild(filesTag);
        }

        // 3. Convert to Raw String (No indentation, no XML declaration)
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    /**
     * This method transform the database stored {@link ItemSnapshot} content into a list of {@link SnapshotElement}
     * Deserialized SnapshotElement are returned AND are stored into `snapshot` parameter object, REPLACING any
     * element it was already holding. Calling this method twice on the same snapshot is therefore harmless.
     * @param snapshot the snapshot to analyze
     * @return the list of deserialize snapshot elements
     */
    public List<SnapshotElement> deserialize(ItemSnapshot snapshot) throws Exception {
        if (snapshot == null || StringUtils.isBlank(snapshot.getContent())) {
            return List.of();
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(snapshot.getContent())));
        document.getDocumentElement().normalize();

        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();
        XPathExpression expr = xpath.compile("//*[local-name()='MetadataField' or local-name()='File']");

        NodeList nodes = (NodeList) expr.evaluate(document, XPathConstants.NODESET);
        List<SnapshotElement> elements = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            elements.add(elementFactory.parse((Element) nodes.item(i)));
        }
        snapshot.setSnapshotElements(elements);
        return elements;
    }

}
