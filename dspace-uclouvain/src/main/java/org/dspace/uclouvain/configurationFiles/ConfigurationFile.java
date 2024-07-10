package org.dspace.uclouvain.configurationFiles;

import java.io.IOException;
import java.util.List;

// Main interface for the configuration files.
public interface ConfigurationFile <T>{
    byte[] getData() throws IOException;
    void loadData();
    T get(String key);
    List<T> get(List<String> key);
    String getPath();
    String getName();
}
