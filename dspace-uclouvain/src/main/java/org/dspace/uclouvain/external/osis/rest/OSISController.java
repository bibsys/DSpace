package org.dspace.uclouvain.external.osis.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.MetadataSelectFieldValuesGenerator;
import org.dspace.uclouvain.external.osis.client.OSISClientImpl;
import org.dspace.uclouvain.external.osis.model.OSISStudentDegree;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;

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

    /** 
     * When calling /api/uclouvain/osis/student/{fgs}/info/all with a given FGS identifier,
     * returns all information about the corresponding student studies.
     * 
     * @param fgs: The fgs identifier of the student
     * @return: A list of containing student's degree information
     */
    @RequestMapping(method = RequestMethod.GET, value = "/student/{fgs}/info/all")
    public OSISStudentDegree[] getAllStudentInfoByFGS(@PathVariable String fgs) {
        return this.osisClient.getOSISStudentDegreeByFGS(fgs);
    }

    /** 
     * When calling /api/uclouvain/osis/student/{fgs}/info/degree with a given FGS,
     * returns the degree information of the corresponding student.
     * 
     * @param fgs: The fgs identifier of the student
     * @return: A list containing all the degree codes for given FGSs
     */
    @RequestMapping(method = RequestMethod.GET, value = "/student/{fgs}/info/degree")
    public List<HashMap<String, String>> getStudentDegreeCodesByFGS(@PathVariable String fgs) {
        List<HashMap<String, String>> returnValueArray  = new ArrayList<HashMap<String, String>>();
        OSISStudentDegree[] osisStudentDegreeResponse = this.osisClient.getOSISStudentDegreeByFGS(fgs);
        for (OSISStudentDegree degree: osisStudentDegreeResponse){
            HashMap<String, String> returnValueMap = new HashMap<>();
            returnValueMap.put("fgs", fgs);
            if(!degree.isError()){
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
     * Generate a List that contains the metadata value to be modified and its value/options
     * 
     * @param fgs: The fgs identifier of the student
     * @param degreeTypeFilter: Precise the type of the degree to retrieve 
     * @return List<HashMap<String, String>>
     */
    @RequestMapping(method = RequestMethod.GET, value = "/students/info/degree")
    public HashMap<String, MetadataSelectFieldValuesGenerator.OSISStudentMetadataContent>
           getStudentsDegreeCodesByFGS(@RequestParam List<String> fgs, @RequestParam String degreeTypeFilter){

        MetadataSelectFieldValuesGenerator selectFieldValues =
                new MetadataSelectFieldValuesGenerator("data-" + DEGREE_CODE_FIELD);
        for(String fgs_id: fgs){
            OSISStudentDegree[] osisStudentDegreeResponse = this.getAllStudentInfoByFGS(fgs_id);
            for (OSISStudentDegree studentDegree: osisStudentDegreeResponse){
                String degreeCode = studentDegree.getSigleOffreCompletN();
                String degreeLabel = studentDegree.getIntitOffreComplet();
                String category = studentDegree.getCategorieDecret();
                if (!studentDegree.isError() && degreeCode != null && !degreeCode.isBlank()
                        && degreeLabel != null && !degreeLabel.isBlank()
                        && category != null && !category.isBlank()
                        && category.toLowerCase().equals(degreeTypeFilter.toLowerCase())) {
                    String displayed = String.join(DEGREE_PART_SEPARATOR, Arrays.asList(degreeCode, degreeLabel));
                    selectFieldValues.addMetadataContentElementOption(degreeCode, displayed);
                }
            }
        }
        return selectFieldValues.generateResponse();
    }
}
