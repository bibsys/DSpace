/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.step;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * This is class represents the data structure for a DataChangeRequest.
 * NOTE: We need to use @JsonUnwrapped to avoid having a 'null' value in the request response for this specific field.
 * Instead, it will return an empty object '{}', which is understood by the frontend has a hint to not display
 * the section.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class DataChangeRequest implements SectionData {

    @JsonUnwrapped
    private String changeData;

    public String getChangeData() {
        return changeData;
    }
    public void setChangeData(String changeData) {
        this.changeData = changeData;
    }
}
