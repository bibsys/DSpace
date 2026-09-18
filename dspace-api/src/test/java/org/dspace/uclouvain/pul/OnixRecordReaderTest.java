/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jdom2.JDOMException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * {@link OnixRecordReader} on the sample records: what the import needs to decide is read from the DIM.
 */
public class OnixRecordReaderTest {

    private static final Path STYLESHEET = Path.of("..", "dspace", "config", "crosswalks", "onix-submission.xsl");
    private static final Path SAMPLES = Path.of("src", "test", "data", "pul");

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private final OnixRecordReader reader = new OnixRecordReader(STYLESHEET.toFile());

    @Test
    public void readsIdentifiersTitleAndNotificationType() throws Exception {
        OnixRecord record = reader.read(SAMPLES.resolve("29303100021680.xml").toFile());
        assertEquals("29303100021680", record.gcoi());
        assertEquals(List.of("9782875584533", "9782875584526"), record.isbns());
        assertTrue(record.title().startsWith("Mons dans la tourmente"));
        assertEquals("03", record.notificationType());
        assertFalse(record.isDeletion());
        assertFalse(record.unnamedContributors());
        // HIGHQ (1000 px) and THUMBNAIL (125 px) are offered: the smallest is kept
        assertEquals("https://pul.uclouvain.be/resources/titles/29303100021680/images/"
            + "477bdb55b231264bb53a7942fd84254d/THUMBNAIL/9782875584533.jpg", record.coverUrl());
        assertNotNull(record.dim());
        assertEquals("dim", record.dim().getName());
    }

    @Test
    public void everySampleHasAGcoiAndAnIsbn() throws Exception {
        File[] files = SAMPLES.toFile().listFiles((dir, name) -> name.endsWith(".xml"));
        assertEquals(5, files.length);
        for (File file : files) {
            OnixRecord record = reader.read(file);
            assertEquals(file.getName(), record.gcoi() + ".xml");
            assertFalse(file.getName(), record.isbns().isEmpty());
        }
    }

    @Test
    public void unnamedContributorsAreFlagged() throws Exception {
        assertTrue(reader.read(SAMPLES.resolve("29303100808420.xml").toFile()).unnamedContributors());
    }

    @Test
    public void deletionNotificationIsRecognized() throws Exception {
        String onix = Files.readString(SAMPLES.resolve("29303100971260.xml"))
            .replace("<NotificationType>03</NotificationType>", "<NotificationType>05</NotificationType>");
        File deleted = temporaryFolder.newFile("29303100971260.xml");
        Files.writeString(deleted.toPath(), onix);

        assertTrue(reader.read(deleted).isDeletion());
    }

    @Test
    public void malformedXmlIsRejected() throws Exception {
        File broken = temporaryFolder.newFile("broken.xml");
        Files.writeString(broken.toPath(), "<ONIXMessage><Product>");

        assertThrows(JDOMException.class, () -> reader.read(broken));
    }
}
