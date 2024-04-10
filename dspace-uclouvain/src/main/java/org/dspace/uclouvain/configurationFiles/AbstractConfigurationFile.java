package org.dspace.uclouvain.configurationFiles;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base abstract class for the `ConfigurationFile` interface.
 * When adding a new custom `ConfigurationFile` class, you must inherit from this class 
 * && implement the abstract methods.
 */
public abstract class AbstractConfigurationFile<T> implements ConfigurationFile<T> {

    // ATTRIBUTES
    @Autowired
    protected String source = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir");
    protected byte[] cache;
    protected File configFile;
    protected long lastModified;
    protected String path;

    /**
     * CONSTRUCTOR:
     * Create a new File object from the given path.
     * Use this object to find out if the file exists and assure that it can be read.
     * @param path: The relative path to the file.
     * @throws IOException
     */
    public AbstractConfigurationFile(String path) throws IOException {
        this.path = path;
        this.configFile = new File(this.source + this.path);
        if (!this.configFile.exists() || !this.configFile.canRead()){
            throw new IOException("Could not read the file because it does not exist or it cannot be read.");
        }
        this.lastModified = -1;
    }

    // METHODS

    /**
     * Called to access the data from the file.
     * First check for new file version (update if needed) then return the data.
     * 
     * @return The data from the file.
     * @throws IOException
     */
    public byte[] getData() throws IOException {
        this.reloadData();
        return this.cache;
    }

    /**
     * Checks if a data reload is required by checking the last modified date of the file.
     * If the file has been modified, reload the data && update 'this.lastModified'.
     * @throws IOException
     */
    protected void reloadData() throws IOException {
        if (this.configFile.lastModified() > this.lastModified) {
            this.cache = Files.readAllBytes(Path.of(this.configFile.getAbsolutePath()));
            this.lastModified = this.configFile.lastModified();
            this.loadData();
        }
    }

    public String getPath(){
        return this.path;
    }

    // ABSTRACT METHODS THAT NEEDS TO BE IMPLEMENTED BY CHILD CLASSES
    public abstract void loadData();
    public abstract T get(String key);
    public abstract List<T> get(List<String> key);
    public abstract String getName();
}

