/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.rest;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.MetadataSelectFieldValuesGenerator;
import org.dspace.uclouvain.external.osis.client.OSISClientImpl;
import org.dspace.uclouvain.external.osis.model.OSISStudentDegree;
import org.eclipse.jetty.http.HttpStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 
* Main Controller for uclouvain/osis endpoint
*/
@RestController
@RequestMapping("/api/uclouvain/osis")
public class OSISController {

    @Autowired
    private OSISClientImpl osisClient;

    public static final String DEGREE_PART_SEPARATOR = " - ";

    private final String DEGREE_CODE_FIELD = DSpaceServicesFactory.getInstance().getConfigurationService()
            .getProperty("uclouvain.global.metadata.degreecode.field", "masterthesis.degree.code");

    private final Logger logger = LogManager.getLogger(OSISController.class);

    /** 
     * When calling /api/uclouvain/osis/student/{fgs}/info/degree with a given FGS,
     * returns the degree information of the corresponding student.
     * 
     * @param fgs The fgs identifier of the student.
     * @return A list containing all the degree codes for given FGSs.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/student/{fgs}/info/degree")
    public List<HashMap<String, String>> getStudentDegreeCodesByFGS(@PathVariable String fgs) {
        List<HashMap<String, String>> returnValueArray  = new ArrayList<>();
        OSISStudentDegree[] osisStudentDegreeResponse = osisClient.getOSISStudentDegreeByFGS(fgs);
        for (OSISStudentDegree degree: osisStudentDegreeResponse) {
            HashMap<String, String> returnValueMap = new HashMap<>();
            returnValueMap.put("fgs", fgs);
            if (!degree.isError()) {
                String degreeCode = degree.getSigleOffreRacine();
                String degreeLabel = degree.getIntitOffreComplet();
                returnValueMap.put("category", degree.getCategorieDecret());
                returnValueMap.put("degreeCode", degreeCode);
                returnValueMap.put("degreeLabel", degreeLabel);
                returnValueMap.put("degreeDisplayValue",
                        String.join(DEGREE_PART_SEPARATOR, Arrays.asList(degreeCode, degreeLabel)));
                returnValueArray.add(returnValueMap);
            }
        }
        return returnValueArray;
    }

    /**
     * Generate a List that contains the metadata value to be modified and its value/options.
     * 
     * @param fgs The fgs identifiers of the students.
     * @param filters A list of parameters to filter the results.
     * @return An object that contains the degree option for the given fgs.
     */
    @RequestMapping(method = RequestMethod.GET, value = "/students/info/degree")
    public ResponseEntity<HashMap<String, MetadataSelectFieldValuesGenerator.OSISStudentMetadataContent>>
           getStudentsDegreeCodesByFGS(@RequestParam List<String> fgs, @RequestParam Map<String, String> filters) {

        // By default, filters takes all the parameters in the Map.
        // Remove fgs from the Map since it is not a custom filter.
        filters.remove("fgs");

        if (!validateFilters(filters)) {
            // Return an error 400 since the given filters are not valid.
            return ResponseEntity.status(HttpStatus.BAD_REQUEST_400).body(null);
        }

        // Initialize the response object.
        MetadataSelectFieldValuesGenerator selectFieldValues =
                new MetadataSelectFieldValuesGenerator("data-" + DEGREE_CODE_FIELD);

        for (String fgs_id: fgs) {
            // For each sgs, find the related degrees.
            Arrays.stream(osisClient.getOSISStudentDegreeByFGS(fgs_id))
                .filter(degree -> isDegreeValid(degree) && evaluateFilters(degree, filters))
                .forEach(degree -> {
                    String degreeCode = degree.getSigleOffreCompletN();
                    String degreeLabel = degree.getIntitOffreComplet();
                    String displayed = String.join(
                        DEGREE_PART_SEPARATOR, Arrays.asList(degreeCode, degreeLabel)
                    );
                    selectFieldValues.addMetadataContentElementOption(degreeCode, displayed);
                });
        }
        return ResponseEntity.ok(selectFieldValues.generateResponse());
    }

    /**
     * Check that a filter map is valid based on the class configuration.
     * If the map contains a key that is not present in the configured list, it returns false.
     * If all the keys are good, return true.
     * 
     * @param filters A map containing the filters and their value.
     * @return True if filters key are valid, else false.
     */
    private boolean validateFilters(Map<String, String> filters) {
        List<String> exposedFields = Arrays.stream(OSISStudentDegree.class.getDeclaredFields())
            .map(field -> field.getName())
            .collect(Collectors.toList());

        return !filters.keySet().stream()
            .anyMatch(filter -> !exposedFields.contains(filter));
    }

    /**
     * Check that a degree retrieved from osis is valid.
     * A degree is valid if it has no errors and the following data:
     *  - a 'sigleOffreCompletN',
     *  - a 'sigleOffreComplet'
     * 
     * @param degree The degree object to check the validity of.
     * @return True if the degree object is valid, false otherwise.
     */
    private boolean isDegreeValid(OSISStudentDegree degree) {
        return !degree.isError()
            && isNotBlank(degree.getSigleOffreCompletN())
            && isNotBlank(degree.getIntitOffreComplet());
    }

    /**
     * Test the given filters for a specific degree object.
     * If one of the filter is not corresponding to the data, return false.
     * If all filters pass, return true.
     * 
     * EX: If we have a degree object with the following data:
     *  {
     *      "sigleOffreCompletN": "MD2MS/G",
     *      "cycle": "2",
     *      "anac": "2019"
     *  }
     * and we give a filter map of: {"cycle": "2"}, this method will return true.
     * 
     * However, if we give the following filter map: {"cycle": "2", "anac": "2023"}, it will return false.
     * 
     * @param degree The degree class to check.
     * @param filters The filters to validate in order for the degree object to be valid.
     * @return True if the degree object is 'compliant' to the given filters, false otherwise.
     */
    private boolean evaluateFilters(OSISStudentDegree degree, Map<String, String> filters) {
        // Loop over the keys/values in the map and check if a corresponding field is present in the object.
        for (String key: filters.keySet()) {
            String value = filters.get(key);
            try {
                // Get the descriptor from the OSISStudentDegree class for the given property.
                PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor(OSISStudentDegree.class, key);
                if (descriptor == null ||  descriptor.getReadMethod() == null ) {
                    return false;
                }
                Object propertyValue = descriptor.getReadMethod().invoke(degree);
                if (propertyValue == null || !propertyValue.toString().equals(value)) {
                    return false;
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                logger.warn(
                    "Could not access given field: " + key + " in given degree object, thrown a " + e.getClass(),
                    e
                );
                return false;
            }
        }
        return true;
    }
}
