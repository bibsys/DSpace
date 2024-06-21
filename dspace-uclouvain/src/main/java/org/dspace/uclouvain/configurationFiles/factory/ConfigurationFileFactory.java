package org.dspace.uclouvain.configurationFiles.factory;

import org.dspace.uclouvain.configurationFiles.ConfigurationFile;

public interface ConfigurationFileFactory {
    ConfigurationFile<?> getConfigurationFile(Class klass);
}