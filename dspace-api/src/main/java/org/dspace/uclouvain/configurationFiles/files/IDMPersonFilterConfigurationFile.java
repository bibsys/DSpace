/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.configurationFiles.files;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.uclouvain.configurationFiles.AbstractConfigurationFile;

/**
 * Load a configuration to know which IDM ids are considered valid for DIAL.pr.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class IDMPersonFilterConfigurationFile extends AbstractConfigurationFile<List<Integer>> {

    private static final Logger log = LogManager.getLogger(IDMPersonFilterConfigurationFile.class);

    // CONSTRUCTOR ============================================================
    public IDMPersonFilterConfigurationFile(String filePath) throws IOException {
        super(filePath);
    }

    /**
     * Load the IDM ids from the configuration file.
     * The configuration file must have the following structure:
     * {
     *  "filters": [
     *      ***PUT_THE_IDM_IDS_HERE***
     *  ]
     * }
     */
    @Override
    public void loadData() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonFilters = mapper.readTree(getRawData()).findValue("filters");
            List<Integer> IDMFilters = Arrays.asList(mapper.treeToValue(jsonFilters, Integer[].class));
            data = IDMFilters;
        } catch (Exception e) {
            log.error("Unable to load IDM filters from configuration file : " + e.getMessage());
        }
    }
}
