package org.dspace.uclouvain.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** 
 * Represents a response sent to the frontend and that can be used by dynamic fields 
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class MetadataSelectFieldValuesGenerator {
    
    private String metadata;
    private OSISStudentMetadataContent content;

    public MetadataSelectFieldValuesGenerator(String metadata){
        this.metadata = metadata;
        this.content = new OSISStudentMetadataContent();
    }

    /** 
     * Adds a new option to the select field
     * 
     * @param optionValue: The value that will be used for processing 
     * @param optionDisplayed: The value that will be displayed to the end user 
     */
    public void addMetadataContentElementOption(String optionValue, String optionDisplayed){
        content.addOption(optionValue, optionDisplayed);
    }

    public void setMetadataContentElementValue(String value, String displayedValue){
        content.setValue(value, displayedValue);
    }

    public HashMap<String, OSISStudentMetadataContent> generateResponse(){
        HashMap<String, OSISStudentMetadataContent> responseHashMap =  new HashMap<>();
        responseHashMap.put(this.metadata, this.content);
        return responseHashMap;
    }

    public boolean optionsPresent(){
        return content.getOptions().size() > 0;
    }

    public String getMetadata(){
        return this.metadata;
    }

    public OSISStudentMetadataContent getContent(){
        return this.content;
    }

    /** 
     * Represents the actual value and options for a given metadata select field.
     * 'value', if set, it fills the select field value 
     * 'options', represents the list of options for the select field's options
     */
    public class OSISStudentMetadataContent {
        private OSISStudentMetadataContentData value = new OSISStudentMetadataContentData();
        private List<OSISStudentMetadataContentData> options = new ArrayList<OSISStudentMetadataContentData>();

        public void setValue(String value, String displayedValue){
            this.value.setValue(value);
            this.value.setDisplayed(displayedValue);
        }

        public OSISStudentMetadataContentData getValue(){
            return this.value;
        }

        public void addOption(String optionValue, String optionDisplayed){
            this.options.add(new OSISStudentMetadataContentData(optionValue, optionDisplayed));
        }

        public List<OSISStudentMetadataContentData> getOptions(){
            return this.options;
        }
    }

    /** 
     * Represents a structure of form: '{"value": "toto", "displayed": "Hello TOTO"}'. 
     * Can be used for both value and options of a metadata select field.
     */
    public class OSISStudentMetadataContentData {
        private String value;
        private String displayed;

        public OSISStudentMetadataContentData(){}

        public OSISStudentMetadataContentData(String value, String displayed){
            this.value = value;
            this.displayed = displayed;
        }

        public void setValue(String value){
            this.value = value;
        }

        public void setDisplayed(String displayed){
            this.displayed = displayed;
        }

        public String getValue(){
            return this.value;
        }

        public String getDisplayed(){
            return this.displayed;
        }
    }
}
