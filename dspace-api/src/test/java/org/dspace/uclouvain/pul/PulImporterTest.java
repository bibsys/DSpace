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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import org.dspace.uclouvain.pul.script.PulImport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * The pure helpers of the import: HTML stripping of the ONIX descriptions, the archiving of processed files and the
 * extensions of the files to attach.
 */
public class PulImporterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void stripsTagsKeepsParagraphsAsNewLinesAndDecodesEntities() {
        String onixText = "<p>Le jeu en vaut-il la chandelle&nbsp;?<br />Depuis plusieurs ann&eacute;es, "
            + "les <b>institutions</b> l&#39;utilisent.</p><p>Second paragraphe.</p>";
        assertEquals("Le jeu en vaut-il la chandelle ?\nDepuis plusieurs années, les institutions l'utilisent.\n"
            + "Second paragraphe.", PulImporter.stripHtml(onixText));
    }

    @Test
    public void plainTextIsLeftAlone() {
        assertEquals("Plain text, with 2 < 3 kept.", PulImporter.stripHtml("Plain text, with 2 < 3 kept."));
        assertNull(PulImporter.stripHtml(null));
        assertEquals("", PulImporter.stripHtml("<p>  </p>"));
    }

    @Test
    public void onlyPdfAndEpubFilesAreAttachable() {
        assertTrue(PulImporter.isAttachableFile(new File("29303100000001.pdf")));
        assertTrue(PulImporter.isAttachableFile(new File("29303100000001.epub")));
        assertTrue(PulImporter.isAttachableFile(new File("978-2-87558-999-0.PDF")));
        assertFalse(PulImporter.isAttachableFile(new File("29303100000001.epub.invalid")));
        assertFalse(PulImporter.isAttachableFile(new File("29303100000001.pdf.invalid")));
        assertFalse(PulImporter.isAttachableFile(new File("29303100000001.xml")));
        assertFalse(PulImporter.isAttachableFile(new File("pdf")));
        assertEquals("not a .epub or .pdf file",
            Outcome.skipped(new File("x.epub.invalid"), PulImporter.expectedExtensions()).message());
    }

    @Test
    public void processedFileMovesToTheSubdirectoryAndReplacesAnOlderCopy() throws Exception {
        File directory = temporaryFolder.newFolder("onix");
        File file = new File(directory, "29303100000001.xml");
        Files.writeString(file.toPath(), "new");
        Path done = directory.toPath().resolve("done");
        Files.createDirectories(done);
        Files.writeString(done.resolve(file.getName()), "old");

        Path moved = PulImport.moveToSubdirectory(file, "done");

        assertFalse(file.exists());
        assertEquals(done.resolve(file.getName()), moved);
        assertEquals("new", Files.readString(moved));
        assertFalse(new File(directory, "errors").exists());
    }
}
