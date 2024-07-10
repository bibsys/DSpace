package org.dspace.uclouvain.configurationFiles.factory;

import java.util.List;

import org.dspace.uclouvain.configurationFiles.ConfigurationFile;

public class ConfigurationFileFactoryImpl implements ConfigurationFileFactory {
    private List<ConfigurationFile<?>> configurationFiles;

    /**
     * Returns a ConfigurationFile class for the given configuration file path.
     */
    public ConfigurationFile<?> getConfigurationFile(Class klass){
        for (ConfigurationFile<?> cf: this.configurationFiles){
            if (cf.getClass() == klass){
                return cf;
            }
        }
        return null;
    }

    // GETTERS && SETTERS
    public List<ConfigurationFile<?>> getConfigurationFiles(){
        return this.configurationFiles;
    }

    public void setConfigurationFiles(List<ConfigurationFile<?>> configurationFiles) {
        this.configurationFiles = configurationFiles;
    }
}
