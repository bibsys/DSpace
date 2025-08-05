/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.dspace.app.rest.RestResourceController;

/**
 * An entry in a Vocabulary
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class VocabularyEntryRest extends RestAddressableModel {
    public static final String NAME = "vocabularyEntry";
    public static final String PLURAL_NAME = "vocabularyEntries";
    public static final String CATEGORY = RestAddressableModel.SUBMISSION;

    @JsonInclude(Include.NON_NULL)
    private String authority;
    private String display;
    private String value;
    private Map<String, String> otherInformation;
    private String source;

    /**
     * The Vocabulary Entry Details resource if available related to this entry
     */
    @JsonIgnore
    private VocabularyEntryDetailsRest vocabularyEntryDetailsRest;

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String value) {
        this.display = value;
    }

    public Map<String, String> getOtherInformation() {
        return otherInformation;
    }

    public void setOtherInformation(Map<String, String> otherInformation) {
        this.otherInformation = otherInformation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }

    public void setVocabularyEntryDetailsRest(VocabularyEntryDetailsRest vocabularyEntryDetailsRest) {
        this.vocabularyEntryDetailsRest = vocabularyEntryDetailsRest;
    }

    public VocabularyEntryDetailsRest getVocabularyEntryDetailsRest() {
        return vocabularyEntryDetailsRest;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String getType() {
        return VocabularyEntryRest.NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }
}
