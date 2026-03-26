/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.dspace.uclouvain.configurationFiles.files.IDMPersonFilterConfigurationFile;
import org.dspace.uclouvain.external.esb.client.ESBClient;
import org.dspace.uclouvain.external.esb.model.responses.ESBPersonIDMMembershipResponse;
import org.dspace.uclouvain.profileIngester.exceptions.IDMCheckException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service to validate the IDM membership of a person.
 * A person is considered to have a valid IDM membership if one of its IDM entry is part of the configured entries.
 * See {@link IDMPersonFilterConfigurationFile} for configuration.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class IDMPersonValidityServiceImpl implements IDMPersonValidityService {
    @Autowired
    ESBClient esbClient;
    @Autowired
    IDMPersonFilterConfigurationFile idmPersonFilterConfigurationFile;

    /**
     * Check if a given person has a valid IDM membership.
     * 
     * @param fgs the FGS identifier of the person to check validity of.
     * @return true if the user has a valid IDM membership, false otherwise.
     */
    public boolean isPersonIDMValid(String fgs) throws IDMCheckException {
        List<Integer> idmEntries = getIDMEntriesForFGS(fgs);
        return isPersonIDMValid(idmEntries);
    }

    public boolean isPersonIDMValid(List<Integer> idmEntries) throws IDMCheckException {
        if (!idmEntries.isEmpty()) {
            try {
                List<Integer> filters = idmPersonFilterConfigurationFile.getData();
                return filters.stream().anyMatch(filter -> idmEntries.contains(filter));
            } catch (IOException e) {
                throw new IDMCheckException("Could not read IDM filters config file.", e);
            }
        }
        return false;
    }

    /**
     * Get a list of all the IDM ids of which a given user is a member of.
     * 
     * @param fgs The FGS identifier of the user to get IDM information of.
     * @return A list of IDM ids of which the user is a member of.
     */
    public List<Integer> getIDMEntriesForFGS(String fgs) throws IDMCheckException {
        ESBPersonIDMMembershipResponse[] memberships = esbClient.getIDMMembershipsForFGS(fgs);
        if (memberships == null) {
            throw new IDMCheckException("No valid response from ESB for IDM check.");
        }
        return Arrays.stream(memberships).map(membership -> membership.getGridNumber()).toList();
    }
}
