/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.configurationFiles.factory;

import java.util.List;

import org.dspace.uclouvain.configurationFiles.ConfigurationFile;

public class ConfigurationFileFactoryImpl implements ConfigurationFileFactory {

    private List<ConfigurationFile<?>> configurationFiles;

    /** Returns a ConfigurationFile class for the given configuration file path. */
    public ConfigurationFile<?> getConfigurationFile(String name) {
        return configurationFiles
                .stream()
                .filter(cf -> cf.getName().equals(name))
                .findFirst().orElse(null);
    }

    // GETTERS && SETTERS
    public List<ConfigurationFile<?>> getConfigurationFiles() {
        return this.configurationFiles;
    }

    public void setConfigurationFiles(List<ConfigurationFile<?>> configurationFiles) {
        this.configurationFiles = configurationFiles;
    }
}
