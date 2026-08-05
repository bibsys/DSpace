/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;

import org.dspace.uclouvain.content.snapshot.element.FileSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.MetadataSnapshotElement;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit tests about {@link org.dspace.uclouvain.content.snapshot.element.SnapshotElement} identity.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class SnapshotElementTest {

    private Element xmlElement(String tagName, String[]... attributes) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element element = document.createElement(tagName);
        for (String[] attribute : attributes) {
            element.setAttribute(attribute[0], attribute[1]);
        }
        return element;
    }

    /**
     * NON-REGRESSION TEST :: equal elements MUST share their hash code.
     *
     * The very same element carries an `attributes` map when it is read back from the stored XML, but not when it was
     * just built in memory. Hashing that map therefore gave two different hash codes to two elements that `equals`
     * each other -- breaking the contract, and any hash-based collection built on those elements.
     */
    @Test
    public void testEqualElementsShareTheirHashCodeAcrossRepresentations() throws Exception {
        MetadataSnapshotElement inMemory = new MetadataSnapshotElement("dc.title[0]", "Lorem ipsum");

        Element xml = xmlElement("MetadataField", new String[] {"path", "dc.title[0]"});
        xml.setTextContent("Lorem ipsum");
        MetadataSnapshotElement deserialized = new MetadataSnapshotElement(xml);

        assertEquals("both representations describe the same element", inMemory, deserialized);
        assertEquals("equal elements must share their hash code", inMemory.hashCode(), deserialized.hashCode());
    }

    /** Two occurrences of a same field differ, and an element only equals another of the very same type */
    @Test
    public void testElementsAreDistinguishedByPathValueAndType() throws Exception {
        MetadataSnapshotElement title = new MetadataSnapshotElement("dc.title[0]", "Lorem ipsum");

        assertNotEquals("a different value means a different element",
            title, new MetadataSnapshotElement("dc.title[0]", "Dolor sit amet"));
        assertNotEquals("a different occurrence means a different element",
            title, new MetadataSnapshotElement("dc.title[1]", "Lorem ipsum"));

        UUID bitstreamId = UUID.randomUUID();
        FileSnapshotElement file = new FileSnapshotElement(bitstreamId, "report.pdf", "MD5#abc", "openaccess");
        assertNotEquals("a file is never equal to a metadata", title, file);
    }

    /**
     * The key must discriminate on the type too, since nothing forbids a metadata occurrence and a bitstream from
     * ending up with the same path.
     */
    @Test
    public void testKeyDiscriminatesOnTypeAndPath() {
        UUID bitstreamId = UUID.randomUUID();
        FileSnapshotElement file = new FileSnapshotElement(bitstreamId, "report.pdf", "MD5#abc", "openaccess");
        MetadataSnapshotElement homonym = new MetadataSnapshotElement(bitstreamId.toString(), "whatever");

        assertEquals("both elements do share the very same path", file.getPath(), homonym.getPath());
        assertNotEquals("...yet they must not share a key", file.getKey(), homonym.getKey());
        assertTrue("the key must keep the path readable", file.getKey().endsWith("::" + bitstreamId));
    }
}
