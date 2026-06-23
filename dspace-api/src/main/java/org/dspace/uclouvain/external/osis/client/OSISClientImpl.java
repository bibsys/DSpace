/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.osis.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.uclouvain.core.GenericHttpClient;
import org.dspace.uclouvain.core.GenericResponse;
import org.dspace.uclouvain.core.utils.DateUtils;
import org.dspace.uclouvain.external.osis.configuration.OSISConfiguration;
import org.dspace.uclouvain.external.osis.model.OSISStudentDegree;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OSISClientImpl implements OSISClient {

    private static Logger logger = LogManager.getLogger(OSISClientImpl.class);

    @Autowired
    private OSISConfiguration osisConfiguration;
    private GenericHttpClient httpClient;

    /** 
     * Retrieve student's degree information from his fgs identifier
     * 
     * @param fgs The fgs identifier of the student
     * @return An array of object representing student's degree information
     */
    @Override
    public OSISStudentDegree[] getOSISStudentDegreeByFGS(String fgs) {
        int currentYear = new DateUtils().getCurrentAcademicYear();
        String url = "/students/v0/" + fgs + "/inscriptions/" + currentYear;
        OSISStudentDegree[] student = {};
        try {
            HttpResponse<String> response = this.httpClient.get(url);
            // Convert JSON String into a java OSIS object
            OSISStudentDegree[] degrees = new GenericResponse(response.body())
                    .extractJsonResponseDataToClass(
                            this.osisConfiguration.getResponseDataKey(),
                            OSISStudentDegree[].class
                    );
            if (degrees != null) {
                student = degrees;
            }
        } catch (IOException | InterruptedException | URISyntaxException e) {
            logger.error(e.getClass().getSimpleName() + "while fetching data :: " + e.getMessage());
            e.printStackTrace(System.err);
        }
        return student;
    }

    // Setters && getters
    public void setHttpClient(GenericHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public GenericHttpClient getHttpClient() {
        return this.httpClient;
    }
}
