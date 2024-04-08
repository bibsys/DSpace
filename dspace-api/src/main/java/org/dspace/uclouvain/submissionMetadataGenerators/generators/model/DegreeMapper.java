package org.dspace.uclouvain.submissionMetadataGenerators.generators.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DegreeMapper {
    @JsonProperty("root-code")
    private String rootDegreeCode;
    @JsonProperty("root-label")
    private String rootDegreeLabel;
    @JsonProperty("faculty-code")
    private String facultyCode;
    @JsonProperty("faculty-name")
    private String facultyName;

    public String getRootDegreeCode() { return rootDegreeCode; }
    public void setRootDegreeCode(String rootDegreeCode) {
        this.rootDegreeCode = rootDegreeCode;
    }

    public String getRootDegreeLabel() {
        return rootDegreeLabel;
    }
    public void setRootDegreeLabel(String rootDegreeLabel) {
        this.rootDegreeLabel = rootDegreeLabel;
    }

    public String getFacultyCode() {
        return facultyCode;
    }
    public void setFacultyCode(String facultyName) {
        this.facultyCode = facultyName;
    }

    public String getFacultyName() {
        return facultyName;
    }
    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }
}
