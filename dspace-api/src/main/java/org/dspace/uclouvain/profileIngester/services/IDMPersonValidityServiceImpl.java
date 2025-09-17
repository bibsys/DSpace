/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.profileIngester.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.uclouvain.configurationFiles.files.IDMPersonFilterConfigurationFile;
import org.dspace.uclouvain.external.esb.client.ESBClient;
import org.dspace.uclouvain.external.esb.model.responses.ESBPersonIDMMembershipResponse;
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

    private static final Logger log = LogManager.getLogger(IDMPersonValidityServiceImpl.class);

    /**
     * Check if a given person has a valid IDM membership.
     * 
     * @param fgs the FGS identifier of the person to check validity of.
     * @return true if the user has a valid IDM membership, false otherwise.
     */
    public boolean isPersonIDMValid(String fgs) {
        List<Integer> idmEntries = getIDMEntriesForFGS(fgs);
        if (!idmEntries.isEmpty()) {
            try {
                List<Integer> filters = idmPersonFilterConfigurationFile.getData();
                return filters.stream().anyMatch(filter -> idmEntries.contains(filter));
            } catch (IOException e) {
                log.error("Could not read IDM filters config file to check for person validity.", e);
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
    public List<Integer> getIDMEntriesForFGS(String fgs) {
        ESBPersonIDMMembershipResponse[] memberships = esbClient.getIDMMembershipsForFGS(fgs);
        return (memberships != null)
            ? Arrays.stream(memberships).map(membership -> membership.getGridNumber()).toList()
            : new ArrayList<>();
    }
}
