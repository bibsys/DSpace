/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.snapshot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.UUID;
import javax.xml.parsers.DocumentBuilderFactory;

import org.dspace.AbstractUnitTest;
import org.dspace.uclouvain.content.snapshot.element.FileSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.MetadataSnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.SnapshotElement;
import org.dspace.uclouvain.content.snapshot.element.SnapshotElementFactory;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class UCLouvainSnapshotFactoryTest extends AbstractUnitTest {

    private SnapshotElementFactory factory;

    @Before
    public void setup() {
        this.factory = new DSpace().getServiceManager().getApplicationContext().getBean(SnapshotElementFactory.class);
    }

    @Test
    public void testSnapshotFactory() throws Exception {
        Document document = DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .newDocument();

        // Test factory for MetadataSnapshotElement
        String mdFieldPath = "foo.bar[0]";
        String mdFieldValue = "Lorem ipsum";

        Element mdField = document.createElement("MetadataField");
        mdField.setAttribute("path", mdFieldPath);
        mdField.setTextContent(mdFieldValue);
        SnapshotElement parsedElement = factory.parse(mdField);

        assertNotNull(parsedElement);
        assertTrue(parsedElement instanceof MetadataSnapshotElement);
        assertEquals(mdFieldPath, parsedElement.getPath());
        assertTrue(parsedElement.toString().contains(mdFieldValue));

        // Test factory for FileSnapshotElement
        UUID id = UUID.randomUUID();
        String fName = "fileName.pdf";
        String fAccess = "basic access condition";
        String fChecksum = "MD5#c6779ec2960296ed9a04f08d67f64422";

        mdField = document.createElement("File");
        mdField.setAttribute("uuid", id.toString());
        mdField.setAttribute("name", fName);
        mdField.setAttribute("checksum", fChecksum);
        mdField.setAttribute("access", fAccess);
        parsedElement = factory.parse(mdField);

        assertNotNull(parsedElement);
        assertTrue(parsedElement instanceof FileSnapshotElement);
        assertEquals(id.toString(), parsedElement.getPath());
        assertEquals(id, ((FileSnapshotElement) parsedElement).getUUID());
        assertEquals(fName, ((FileSnapshotElement) parsedElement).getFilename());
        assertEquals(fChecksum, ((FileSnapshotElement) parsedElement).getChecksum());
        assertEquals(fAccess, ((FileSnapshotElement) parsedElement).getAccess());
    }

}
