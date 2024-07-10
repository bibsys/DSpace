/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.configurationFiles;


import java.io.IOException;

public interface ConfigurationFile<T> {
    void loadData();
    T getData() throws IOException;
}
