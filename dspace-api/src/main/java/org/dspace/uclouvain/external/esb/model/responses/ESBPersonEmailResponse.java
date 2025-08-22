/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.esb.model.responses;

/**
 * A model representing a response from Digit email api.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ESBPersonEmailResponse extends ESBPersonResponse {
    private String activity;
    private String emailAddress;
    private String sorting;

    // GETTERS AND SETTERS
    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getSorting() {
        return sorting;
    }

    public void setSorting(String sorting) {
        this.sorting = sorting;
    }
}
