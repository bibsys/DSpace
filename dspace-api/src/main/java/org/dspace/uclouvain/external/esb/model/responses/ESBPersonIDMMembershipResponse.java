/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.esb.model.responses;

/**
 * A model representing a response from the IDM person API.
 * 
 * @author Michaël Pourbaix
 */
public class ESBPersonIDMMembershipResponse extends ESBPersonResponse {
    private Integer gridNumber;

    // SETTERS && GETTERS
    public void setGridNumber(Integer gridNumber) {
        this.gridNumber = gridNumber;
    }

    public Integer getGridNumber() {
        return gridNumber;
    }
}
