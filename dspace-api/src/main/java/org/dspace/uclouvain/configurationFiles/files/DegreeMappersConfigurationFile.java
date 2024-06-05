/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.configurationFiles.files;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.uclouvain.configurationFiles.AbstractConfigurationFile;
import org.dspace.uclouvain.core.GenericResponse;
import org.dspace.uclouvain.degreeMappers.model.DegreeMapper;
import org.dspace.uclouvain.degreeMappers.model.DegreeMappers;

/**
 * Configuration file representing the degree mapper configuration.
 * This is basically a JSON file with a list of degree mappers.
 */
public class DegreeMappersConfigurationFile extends AbstractConfigurationFile<DegreeMapper> {

    public static final String NAME = "degreeMappers";
    private static final Logger logger = LogManager.getLogger(DegreeMappersConfigurationFile.class);

    public DegreeMappers degreeMappers;

    public DegreeMappersConfigurationFile(String path) throws IOException {
        super(path);
    }

    /**
     * Load the data from the file as a DegreeMappers object.
     * The data is loaded as a string and then converted to a DegreeMappers object.
     * PROCESS: FILE -> byte[] -> String -> DegreeMappers
     */
    @Override
    public void loadData() {
        try {
            degreeMappers = new GenericResponse(readFileAsString())
                    .extractJsonResponseDataToClass(null, DegreeMappers.class);
        } catch (IOException ioe) {
            logger.error("There was an error loading the data from the configuration file: bidon_path. Error: "
                    + ioe.getMessage());
            degreeMappers = new DegreeMappers();
        }
    }

    /**
     * Get the degree mapper for a given degree code.
     * First check if the file has been modified and update the degreeMappers.
     * Then return the degree mapper for the given degree code.
     *
     * @param key The degree code to search for.
     * @return The degree mapper for the given degree code.
     */
    public DegreeMapper get(String key) {
        try {
            super.reloadData();
            return this.degreeMappers.get(key);
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Same as this.getDegreeMapperForDegreeCode but processes a list of degree codes.
     *
     * @param keys The degree codes to search for.
     * @return The degree mappers for the given degree codes.
     */
    public List<DegreeMapper> get(List<String> keys) {
        try {
            super.reloadData();
            return keys
                .stream()
                .map(key -> this.degreeMappers.get(key))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (IOException ioe) {
            return null;
        }
    }

    /**
     * Read the file content as a string.
     * To do so converts the byte array to a string using UTF-8 encoding.
     *
     * @return The file content as a string.
     * @throws IOException
     */
    public String readFileAsString() throws IOException {
        byte[] data = "sample_data".getBytes();
        return new String(data, StandardCharsets.UTF_8);
    }

    public DegreeMappers getDegreeMappers() {
        return this.degreeMappers;
    }

    public String getName() {
        return NAME;
    }
}
