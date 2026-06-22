/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.esb.client;

import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.uclouvain.core.GenericHttpClient;
import org.dspace.uclouvain.core.GenericResponse;
import org.dspace.uclouvain.core.utils.DateUtils;
import org.dspace.uclouvain.external.esb.model.ESBPersonProfile;
import org.dspace.uclouvain.external.esb.model.responses.ESBPersonAffiliationResponse;
import org.dspace.uclouvain.external.esb.model.responses.ESBPersonEmailResponse;
import org.dspace.uclouvain.external.esb.model.responses.ESBPersonIDMMembershipResponse;
import org.dspace.uclouvain.external.esb.model.responses.ESBPersonMainResponse;

/**
 * ESB client for requests to the ESB. This can call multiple API's (Digit, Organisation...)
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class ESBClientImpl implements ESBClient {

    private static Logger logger = LogManager.getLogger(ESBClientImpl.class);

    private GenericHttpClient httpClient;
    private final String DIGIT_PATH = "/digit/v1";
    private final String EMPLOYEE_PATH = "/employees/v1";

    // ---------- DIGIT ENDPOINTS ----------
    /**
     * Get email information for a given fgs identifier.
     * This endpoint can return multiple emails for a single person.
     * 
     * @param fgs The identifier of the person to get the emails of.
     * @return An array of emails for the given fgs identifier.
     */
    public ESBPersonEmailResponse[] getEmailForFGS(String fgs) {
        String url = DIGIT_PATH + "/persons/" + fgs + "/email";
        ESBPersonEmailResponse[] emails = {};
        try {
            HttpResponse<String> response = httpClient.get(url);
            emails = Optional.ofNullable(
                new GenericResponse(response.body())
                    .extractJsonResponseDataToClass("email", ESBPersonEmailResponse[].class)
            ).orElse(new ESBPersonEmailResponse[0]);
        } catch (Exception e) {
            logger.error("Could not fetch email of person with fgs: " + fgs, e);
        }
        return emails;
    }

    /**
     * Get a single 'main' email address for a given fgs.
     * The best email possible is the one which is 'Actif' and 'Primaire'.
     * If none are found with those criteria, we return the first found email or null if none exist.
     * 
     * @param fgs The fgs to get an email address for.
     */
    public ESBPersonEmailResponse getMainEmailForFGS(String fgs) {
        ESBPersonEmailResponse[] emails = getEmailForFGS(fgs);
        return Arrays.stream(emails)
            .filter(email -> {
                return ESBPersonEmailResponse.ACTIVITY_ACTIVE_EMAIL.equals(email.getActivity())
                    && ESBPersonEmailResponse.SORTING_MAIN_EMAIL.equals(email.getSorting());
            })
            .findFirst()
            .orElse(
                // Get the first email in case a main email is not found.
                Arrays.stream(emails).findFirst().orElse(null)
            );
    }

    /**
     * Get an array of all affiliations for a given person.
     * 
     * @param fgs the fgs identifier of the person to get affiliations of.
     * @return An array of affiliations.
     */
    public ESBPersonAffiliationResponse[] getAffiliationsForFGS(String fgs) {
        String url = EMPLOYEE_PATH + "/" + fgs + "/departments";
        ESBPersonAffiliationResponse[] affiliations = {};
        try {
            HttpResponse<String> response = httpClient.get(url);
            affiliations = Optional.ofNullable(
                new GenericResponse(response.body())
                    .extractJsonResponseDataToClass("department", ESBPersonAffiliationResponse[].class)
            ).orElse(new ESBPersonAffiliationResponse[0]);
        } catch (Exception e) {
            logger.error("Could not fetch affiliations of person with fgs: " + fgs, e);
        }
        return affiliations;
    }

    /**
     * Get main data about a person. Returns many useful data like first and last name, gender...
     * 
     * @param fgs The identifier of the person.
     */
    public ESBPersonMainResponse getDataForFGS(String fgs) {
        String url = DIGIT_PATH + "/persons/" + fgs;
        ESBPersonMainResponse mainData = null;
        try {
            HttpResponse<String> response = httpClient.get(url);
            mainData = new GenericResponse(response.body())
                .extractJsonResponseDataToClass("personalData", ESBPersonMainResponse.class);
        } catch (Exception e) {
            logger.error("Could not fetch main data of person with fgs: " + fgs, e);
        }
        return mainData;
    }

    /**
     * Get a complete profile object for a given person fgs.
     * The object contains all the recoverable data for a person.
     * 
     * @param fgs The identifier of the person.
     */
    public ESBPersonProfile getProfileForFGS(String fgs) {
        // Do the requests to gather information about the person.
        ESBPersonEmailResponse email = getMainEmailForFGS(fgs);
        ESBPersonMainResponse main = getDataForFGS(fgs);
        ESBPersonAffiliationResponse[] affiliations = getAffiliationsForFGS(fgs);

        ESBPersonProfile profileData = new ESBPersonProfile();
        if (email != null) {
            profileData.setEmail(email.getEmailAddress());
        }

        if (main != null) {
            profileData.setFullName(
                // Concatenate last and first name (and avoid empty values).
                Stream.of(main.getLastName(), main.getFirstName())
                    .filter(StringUtils::isNotEmpty)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(null)
            );
            profileData.setBirthDate(getDSpaceDate(main.getBirthDate()));
            if (main.getGender() != null) {
                profileData.setGender(main.getGender().toLowerCase());
            }
            profileData.setTitle(main.getTitle());
        }

        if (affiliations.length > 0) {
            profileData.setAffiliations(
                Stream.of(affiliations).map(aff -> aff.getEntity().getAcronyms()).collect(Collectors.toList())
            );
        }
        return profileData;
    }

    /**
     * Retrieve all the IDM ids for a given person fgs.
     * 
     * @param fgs The FGS identifier of the person to get IDM information of.
     * @return A list of IDM membership object concerning the user.
     */
    public ESBPersonIDMMembershipResponse[] getIDMMembershipsForFGS(String fgs) {
        String url = DIGIT_PATH + "/identityGrid/" + fgs + "/membership";
        ESBPersonIDMMembershipResponse[] memberships = {};
        try {
            HttpResponse<String> response = httpClient.get(url);
            memberships = new GenericResponse(response.body())
                .extractJsonResponseDataToClass("Element", ESBPersonIDMMembershipResponse[].class);
        } catch (Exception e) {
            logger.error("Could not get IDM membership for fgs " + fgs, e);
        }
        return memberships;
    }

    /**
     * Converts a given date to the dspace format.
     * If the conversion fails, it returns the non-converted date + log.
     * 
     * @param date The date to convert to DSpace format.
     * @return The date converted to DSpace format.
     */
    private String getDSpaceDate(String date) {
        try {
            return DateUtils.toDSpaceDate(date, "dd/MM/yyyy");
        } catch (ParseException e) {
            logger.warn("Could not parse date" + date + " to DSpace format, adding it like it is.");
            return date;
        }
    }

    // GETTERS && SETTERS
    public GenericHttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpClient(GenericHttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
