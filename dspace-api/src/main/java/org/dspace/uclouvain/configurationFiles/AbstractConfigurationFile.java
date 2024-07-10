package org.dspace.uclouvain.configurationFiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
     * @param path: The relative path to the file.
     * @throws IOException if file doesn't exist or cannot be read.
     */
    protected AbstractConfigurationFile(String path) throws IOException {
        Path fullPath = Paths.get(
            DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir"),
            path
        );
        this.configFile = fullPath.toFile();
        if (!this.configFile.exists() || !this.configFile.canRead()){
            throw new IOException("Could not read the file because it does not exist or it cannot be read.");
        }
        this.lastModified = -1;
    }

    // METHODS ================================================================
    public T getData() throws IOException {
        this.reloadData();
        return this.data;
    }

    /**
     * Get raw data from the configuration file ensuring the data is up-to-date
     * with file content.
     * 
     * @return The data from the file.
     * @throws IOException for any system IO errors
     */
    public byte[] getRawData() throws IOException {
        this.reloadData();
        return this.rawData;
    }

    /**
     * Reload data from the configuration file if needed (by checking the last modified date of the file)
     * @throws IOException for any system IO errors
     */
    protected void reloadData() throws IOException {
        if (this.configFile.lastModified() > this.lastModified) {
            this.lastModified = this.configFile.lastModified();
            this.rawData = Files.readAllBytes(Path.of(this.configFile.getAbsolutePath()));
            this.loadData();
        }
    }
}

