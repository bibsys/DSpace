package org.dspace.uclouvain.osis.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.dspace.uclouvain.core.model.MetadataSelectFieldValuesGenerator;
import org.dspace.uclouvain.osis.client.OSISClientImpl;
import org.dspace.uclouvain.osis.model.OSISStudentDegree;
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
                returnValueMap.put("category", degree.getCategorieDecret());
                returnValueMap.put("degreeCode", degree.getSigleOffreRacine() + " - " + degree.getIntitOffreComplet());
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
    public HashMap<String,MetadataSelectFieldValuesGenerator.OSISStudentMetadataContent> getStudentsDegreeCodesByFGS(@RequestParam List<String> fgs, @RequestParam String degreeTypeFilter){

        MetadataSelectFieldValuesGenerator selectFieldValues  = new MetadataSelectFieldValuesGenerator("data-masterthesis.degree.code");
        for(String fgs_id: fgs){
            OSISStudentDegree[] osisStudentDegreeResponse = this.getAllStudentInfoByFGS(fgs_id);
            for (OSISStudentDegree studentDegree: osisStudentDegreeResponse){
                if(!studentDegree.isError() && (studentDegree.getSigleOffreRacine() != null) && studentDegree.getIntitOffreComplet().toLowerCase().contains(degreeTypeFilter.toLowerCase())){
                    String value = studentDegree.getSigleOffreRacine();
                    String displayed = value + " - " + studentDegree.getIntitOffreComplet();
                    selectFieldValues.addMetadataContentElementOption(value, displayed);
                }
            }
        }
        // if(!selectFieldValues.optionsPresent()){
        //     selectFieldValues.setMetadataContentElementValue("NA", "NA - Unknown");
        // }
        // selectFieldValues.addMetadataContentElementOption("NA", "NA - Unknown");

        return selectFieldValues.generateResponse();
    }
}
