package org.dspace.uclouvain.configurationFiles;


import java.io.IOException;

// Main interface for the configuration files.
public interface ConfigurationFile <T>{
    void loadData();
    T getData() throws IOException;
}
