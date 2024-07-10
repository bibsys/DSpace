/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.configurationFiles;

import java.io.IOException;
import java.util.List;

public interface ConfigurationFile<T> {
    byte[] getData() throws IOException;
    void loadData();
    T get(String key);
    List<T> get(List<String> key);
    String getPath();
    String getName();
}
