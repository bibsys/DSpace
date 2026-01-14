/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.export.result;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.jena.atlas.RuntimeIOException;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.integration.crosswalks.ItemExportCrosswalk;
import org.dspace.core.Context;

/**
 * ExportResult implementation that stores the result in a temporary file.
 * NOTE: Although the temp file is created by java, it is not deleted by java automatically.
 * 
 * Authored-by; Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class TempFileExportResult implements ExportResult {

    protected Path filePath;
    protected String mimeType;
    protected String fileName;
    protected long size;

    /**
     * Retrieve an export result based on a temp file. This file can be deleted once the resource has been consumed.
     * @param context The current DSpace application context.
     * @param crosswalk The crosswalk to use to generate the export.
     * @param items The items to inject to the crosswalk in order to generate the export result.
     * @throws CrosswalkException if any exception occurres when generating the export result.
     */
    public TempFileExportResult(
        Context context, ItemExportCrosswalk crosswalk, Iterator<Item> items
    ) throws CrosswalkException {
        try {
            // DEV_NOTE: ThreadLocalRandom can be used to retrieve a thread safe random number.
            Path tempFile = Files.createTempFile(
                "tempExport_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(), ".tmp"
            );
            this.filePath = tempFile;
            this.mimeType = crosswalk.getMIMEType();
            this.fileName = crosswalk.getFileName();
            try (OutputStream output = Files.newOutputStream(tempFile)) {
                crosswalk.disseminate(context, items, output);
            }
            this.size = Files.size(tempFile);
        } catch (Exception e) {
            if (filePath != null) {
                this.close();
            }
            throw new CrosswalkException("Error generating export", e);
        }
    }

    public InputStream readStream() throws IOException {
        return Files.newInputStream(filePath);
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getSize() {
        return size;
    }

    public String getFileName() {
        return fileName;
    }

    /**
     * Delete the temp file from the disk once the resource is consumed.
     */
    public void close() {
        try {
            Files.deleteIfExists(filePath);
        } catch (Exception e) {
            throw new RuntimeIOException("Could not delete export temp file.", e);
        }
    }
}
