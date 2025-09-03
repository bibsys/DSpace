/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.mails.metadataParser;

import java.util.HashMap;

/**
 * Main model config class for a mail metadata parser.
 * This class is used to parse a metadata in order to be displayed in a mail.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class MailMetadataParser {
    private String metadataField;
    private HashMap<String, String> labels = new HashMap<>();
    private String vocabularyName;
    private boolean multipleValues = true;
    private String separator = "; ";

    public String getLabel(String lang) {
        return labels.get(lang);
    }

    // GETTERS AND SETTERS
    public HashMap<String, String> getLabels() {
        return labels;
    }

    public void setLabels(HashMap<String, String> labels) {
        this.labels = labels;
    }

    public String getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

    public String getVocabularyName() {
        return vocabularyName;
    }

    public void setVocabularyName(String vocabularyName) {
        this.vocabularyName = vocabularyName;
    }

    public boolean getMultipleValues() {
        return multipleValues;
    }

    public void setMultipleValues(boolean multipleValues) {
        this.multipleValues = multipleValues;
    }

    public String getSeparator() {
        return separator;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }
}
