package org.dspace.uclouvain.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Class that can be used to deal with the loading of a file's data.
 * This class allows to check for new version of the file through the ´isFileNewer´ method.
 * You can also read file's data through the ´readFileAsString´ && ´readFileAsBytes´ methods.
 */
public class ConfigurationFileLoader {
    // Time in ms of the last modification of the file
    private Long lastModified;
    private String absoluteFilePath;
    public boolean isError = false;

    public ConfigurationFileLoader(String filePath){
        this.absoluteFilePath = filePath;
        this.loadFile();
    }

    /**
     * Read the file and set the 'this.isError' && 'this.lastModified' properties.
     * If the file does not exists or cannot be red, set error to true. 
     * If the file exists and can be red, set the lastModified ms to the file's last modified date.
     */
    private void loadFile(){
        File configurationFile = new File(this.absoluteFilePath);
        if (configurationFile.exists() && configurationFile.canRead()){
            this.lastModified = configurationFile.lastModified();
            this.isError = false;
        } else {
            this.isError = true;
        }
    }

    /**
     * Read the file and returns its content as a String.
     * 
     * @return: A String representing the content of the file or null if we could not read the file.
     * @throws IOException
     */
    public String readFileAsString() throws IOException {
        // Update the 'lastModified' or 'isError' state before reading
        this.loadFile();
        return !this.isError ? Files.readString(Path.of(this.absoluteFilePath)) : null;
    }

    /**
     * Read the file and returns its content as an array of bytes.
     * 
     * @return: An array of bytes representing the content of the file or null if we could not read the file.
     * @throws IOException
     */
    public byte[] readFileAsBytes() throws IOException {
        // Update the 'lastModified' or 'isError' state before reading
        this.loadFile();
        return this.isError? Files.readAllBytes(Path.of(this.absoluteFilePath)) : null;
    }

    /**
     * Public method which indicate if the file has been through modification since the last read.
     * 
     * @return - TRUE if it has been modified since the list read.
     *         - FALSE if it has not been modified since the last read.
     */
    public boolean isFileNewer(){
        if (!this.isError) {
            return (this.lastModified == null ) ? true : new File(this.absoluteFilePath).lastModified() > this.lastModified;
        }
        return false;
    }

    // GETTERS && SETTERS

    public String getAbsoluteFilePath() {
        return this.absoluteFilePath;
    }
}
