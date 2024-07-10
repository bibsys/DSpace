/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.configurationFiles.factory;

import org.dspace.uclouvain.configurationFiles.ConfigurationFile;

public interface ConfigurationFileFactory {
    ConfigurationFile<?> getConfigurationFile(Class klass);
}