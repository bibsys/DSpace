package org.dspace.uclouvain.submissionMetadataGenerators.generators;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.configurationFiles.ConfigurationFile;
import org.dspace.uclouvain.configurationFiles.factory.ConfigurationFileFactory;
import org.dspace.uclouvain.configurationFiles.files.DegreeMappersConfigurationFile;
import org.dspace.uclouvain.submissionMetadataGenerators.generators.model.DegreeMapper;

/**
 * Service to read the degree mappers JSON configuration file.
 */
public class DegreeMappersService {
    
    private static final Logger logger = LogManager.getLogger(DegreeMappersService.class);

    private ConfigurationFile<DegreeMapper> fileLoader;

    // Use this syntax to load bean since @Autowired does not work in this case.
    ConfigurationFileFactory configurationFileFactory = DSpaceServicesFactory
        .getInstance()
        .getServiceManager()
        .getApplicationContext()
        .getBean(ConfigurationFileFactory.class);

    /**
     * CONSTRUCTOR:
     * Load the degree mappers configuration file && keep it as an attribute for later use.
     * If the class is not found in the configuration file, logs a warning.
     */
    @SuppressWarnings("unchecked")
    DegreeMappersService() {
        this.fileLoader = (ConfigurationFile<DegreeMapper>) this.configurationFileFactory.getConfigurationFile(DegreeMappersConfigurationFile.NAME);
        if (fileLoader == null) {
            logger.warn("There was an error instantiating a file loader for the following file: " + this.fileLoader.getPath());
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
        return (fileLoader != null) ? this.fileLoader.get(degreeCode): null;
    }

    /**
     * Same as this.getDegreeMapperForDegreeCode but processes a list of degree codes.
     *
     * @param degreeCodes: The degree codes to search for.
     * @return The degree mappers for the given degree codes.
     */
    public List<DegreeMapper> getDegreeMappersForDegreeCodes(List<String> degreeCodes) {
        return (fileLoader != null) ? this.fileLoader.get(degreeCodes): new ArrayList<DegreeMapper>();
    }
}
