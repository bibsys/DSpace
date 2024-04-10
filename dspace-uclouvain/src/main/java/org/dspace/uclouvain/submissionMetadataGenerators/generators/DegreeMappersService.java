package org.dspace.uclouvain.submissionMetadataGenerators.generators;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.ConfigurationFileLoader;
import org.dspace.uclouvain.core.GenericResponse;
import org.dspace.uclouvain.submissionMetadataGenerators.generators.model.DegreeMapper;
import org.dspace.uclouvain.submissionMetadataGenerators.generators.model.DegreeMappers;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to read the degree mappers JSON data file.
 */
public class DegreeMappersService {
    
    private static final Logger log = LogManager.getLogger();

    private DegreeMappers degreeMappers;

    private ConfigurationFileLoader fileLoader;

    @Autowired
    private String source = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir");


    DegreeMappersService(String filePath) {
        this.fileLoader = new ConfigurationFileLoader(this.source + filePath);
        // At instantiation, load the data a first time
        if (!this.fileLoader.isError) {
            this.readMappingFile();
        } else {
            log.warn("There was an error instantiating a file loader for the following file: " + this.fileLoader.getAbsoluteFilePath());
        }
    }

    /**
     * Load the given file mapping configuration if needed.
     * The file will only be loaded if a newer version of it exists.
     * The version (date) of the file is stored in the lastModified attribute.
     */
    private void loadFileMappers() {
        if (!this.fileLoader.isError) {
            // If there is a new version of the config file, then update 'this.degreeMappers'.
            if (this.fileLoader.isFileNewer()) this.readMappingFile();
        }
        else {
            log.warn("Could not find or read the degree mappers configuration file. Given path: " + this.fileLoader.getAbsoluteFilePath());
            this.degreeMappers = new DegreeMappers();
        }
    }

    /**
     * Read the configuration file and load the data into the degreeMappers attribute.
     */
    private void readMappingFile() {
        try {
            this.degreeMappers = new GenericResponse(this.fileLoader.readFileAsString()).extractJsonResponseDataToClass(null, DegreeMappers.class);
        } catch (IOException e){
            log.warn("Could not read the degree code mappers file or load the data. Given path: " + this.fileLoader.getAbsoluteFilePath(), e);
            this.degreeMappers = new DegreeMappers();
        }
    }

    /**
     * Get the degree mapper for a given degree code.
     * First check if the file has been modified and update the degreeMappers.
     * Then return the degree mapper for the given degree code.
     *
     * @param degreeCode: The degree code to search for.
     * @return The degree mapper for the given degree code.
     */
    public DegreeMapper getDegreeMapperForDegreeCode(String degreeCode) {
        this.loadFileMappers();
        return this.degreeMappers.get(degreeCode);
    }

    /**
     * Same as this.getDegreeMapperForDegreeCode but processes a list of degree codes.
     *
     * @param degreeCodes: The degree codes to search for.
     * @return The degree mappers for the given degree codes.
     */
    public List<DegreeMapper> getDegreeMappersForDegreeCodes(List<String> degreeCodes) {
        this.loadFileMappers();
        return degreeCodes
            .stream()
            .map(degreeCode -> this.degreeMappers.get(degreeCode))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
