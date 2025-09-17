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
 * - Each email has an activity: 'A' is for active and 'I' is for inactive.
 * - Each email has a sorting: 'P' is for primary and 'S' is for secondary.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ESBPersonEmailResponse extends ESBPersonResponse {
    public static final String ACTIVITY_ACTIVE_EMAIL = "A";
    public static final String ACTIVITY_INACTIVE_EMAIL = "I";
    public static final String SORTING_MAIN_EMAIL = "P";
    public static final String SORTING_SECONDARY_EMAIL = "S";

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
