package org.dspace.uclouvain.submissionMetadataGenerators.generators;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.GenericResponse;
import org.dspace.uclouvain.submissionMetadataGenerators.generators.model.DegreeMapper;
import org.dspace.uclouvain.submissionMetadataGenerators.generators.model.DegreeMappers;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to read the degree mappers JSON data file.
 */
public class DegreeMappersService {
    
    private static final Logger log = LogManager.getLogger();

    private String absoluteFilePath;

    private DegreeMappers degreeMappers;

    private Long lastModified;

    @Autowired
    private String source = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir");


    DegreeMappersService(String filePath) {
        this.absoluteFilePath = this.source + filePath;
        this.loadFileMappers(this.absoluteFilePath);
    }

    /**
     * Load the given file mapping configuration if needed.
     * The file will only be loaded if a newer version of it exists.
     * The version (date) of the file is stored in the lastModified attribute.
     *
     * @param filePath: The path to the file to read.
     */
    private void loadFileMappers(String filePath) {
        File mappingFile = new File(filePath);
        if (mappingFile.exists() && mappingFile.canRead()) {
            if (this.isNewVersion(mappingFile)){
                this.readMappingFile(mappingFile);
                this.lastModified = mappingFile.lastModified();
            }
        }
        else {
            log.warn("Could not find or read the degree mappers configuration file. Given path: " + filePath);
            this.degreeMappers = new DegreeMappers();
        }
    }

    /**
     * Read the mapping file and load the data into the degreeMappers attribute.
     *
     * @param mappingFile: The file to read.
     */
    private void readMappingFile(File mappingFile) {
        try {
            String mappingFileString = Files.readString(Path.of(mappingFile.getPath()));
            this.degreeMappers = new GenericResponse(mappingFileString).extractJsonResponseDataToClass(null, DegreeMappers.class);
        } catch (IOException e){
            log.warn("Could not read the degree code mappers file or load the data. Given path: " + mappingFile.getAbsolutePath(), e);
            this.degreeMappers = new DegreeMappers();
        }
    }

    /**
    * Check if the file as been modified since the last time it was read.
    * If the lastModified attribute is null or if the version is newer, it will return true.
    * If the version is the same return false.
    *
    * @param mappingFile: The file to check.
    */
    private boolean isNewVersion(File mappingFile) {
        return (this.lastModified == null) ? true : mappingFile.lastModified() > this.lastModified;
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
        this.loadFileMappers(this.absoluteFilePath);
        return this.degreeMappers.get(degreeCode);
    }

    /**
     * Same as this.getDegreeMapperForDegreeCode but processes a list of degree codes.
     *
     * @param degreeCodes: The degree codes to search for.
     * @return The degree mappers for the given degree codes.
     */
    public List<DegreeMapper> getDegreeMappersForDegreeCodes(List<String> degreeCodes) {
        this.loadFileMappers(this.absoluteFilePath);
        return degreeCodes
            .stream()
            .map(degreeCode -> this.degreeMappers.get(degreeCode))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
