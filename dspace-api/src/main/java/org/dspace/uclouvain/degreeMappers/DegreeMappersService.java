/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.degreeMappers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.uclouvain.configurationFiles.files.DegreeMappersConfigurationFile;
import org.dspace.uclouvain.degreeMappers.model.DegreeMapper;

/**
 * Service to read the degree mappers JSON configuration file.
 */
public class DegreeMappersService {

    private static final Logger logger = LogManager.getLogger(DegreeMappersService.class);

    private DegreeMappersConfigurationFile fileLoader;

    /**
     * CONSTRUCTOR:
     * Load the degree mappers configuration file && keep it as an attribute for later use.
     * If the class is not found in the configuration file, logs a warning.
     */
    @SuppressWarnings("unchecked")
    DegreeMappersService() {
        // empty
    }

    /**
     * Get the degree mapper for a given degree code.
     * First check if the file has been modified and update the degreeMappers.
     * Then return the degree mapper for the given degree code.
     *
     * @param degreeCode The degree code to search for.
     * @return The degree mapper for the given degree code.
     */
    public DegreeMapper getDegreeMapperForDegreeCode(String degreeCode) {
        // empty
        return null;
    }

    /**
     * Same as this.getDegreeMapperForDegreeCode but processes a list of degree codes.
     *
     * @param degreeCodes The degree codes to search for.
     * @return The degree mappers for the given degree codes.
     */
    public List<DegreeMapper> getDegreeMappersForDegreeCodes(List<String> degreeCodes) {
        // empty
        return null;
    }
}