/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.configurationFiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.dspace.services.factory.DSpaceServicesFactory;

/** Base abstract class for the `ConfigurationFile` interface. */
public abstract class AbstractConfigurationFile<T> implements ConfigurationFile<T> {

    // CLASS ATTRIBUTES =======================================================
    protected File configFile;
    protected byte[] rawData;
    protected T data;

    private long lastModified;

    // CONSTRUCTOR ============================================================
    /**
     * Create a new ConfigurationFile object from the given path.
     * @param path The relative path to the file.
     * @throws IOException if the file doesn't exist or cannot be read.
     */
    protected AbstractConfigurationFile(String path) throws IOException {
        Path fullPath = Paths.get(
            DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir"),
            path
        );
        configFile = fullPath.toFile();
        if (!configFile.exists() || !configFile.canRead()) {
            throw new IOException("Could not read the file because it does not exist or it cannot be read.");
        }
        lastModified = -1;
    }

    // METHODS ================================================================
    public T getData() throws IOException {
        reloadData();
        return data;
    }

    /**
     * Get raw data from the configuration file ensuring the data is up-to-date
     * with file content.
     * 
     * @return The data from the file.
     * @throws IOException for any system IO errors
     */
    public byte[] getRawData() throws IOException {
        reloadData();
        return rawData;
    }

    /**
     * Reload data from the configuration file if needed (by checking the last modified date of the file)
     *
     * @throws IOException for any system IO errors
     */
    protected void reloadData() throws IOException {
        if (configFile.lastModified() > lastModified) {
            lastModified = configFile.lastModified();
            rawData = Files.readAllBytes(Path.of(configFile.getAbsolutePath()));
            loadData();
        }
    }

    public abstract void loadData();
}

