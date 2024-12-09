/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of FacultyManagerService
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 *
 */
public class FacultyManagerServiceImpl implements FacultyManagerService {

    @Autowired
    private ResearcherProfileService researcherProfileService;
    private static final Logger log = LogManager.getLogger(FacultyManagerServiceImpl.class);
    private final GroupService groupService = EPersonServiceFactory
            .getInstance()
            .getGroupService();
    private final ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private final String managerGroupName = configService.getProperty("uclouvain.faculty-manager.group", "manager");

    @Override
    public List<EPerson> getFacultyManagers(Context context) throws SQLException {
        return groupService
                .findByName(context, managerGroupName)
                .getMembers().stream()
                .filter(person -> hasManagedDegrees(context, person))
                .collect(Collectors.toList());
    }

    @Override
    public List<EPerson> getFacultyManagers(Context context, String degreeCode) throws SQLException {
        return groupService
                .findByName(context, managerGroupName)
                .getMembers().stream()
                .filter(person -> isManagerFor(context, person, degreeCode))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isFacultyManager(Context context, EPerson person) throws SQLException {
        if (person == null) {
            return false;
        }
        // Determine if the person is a member of the "manager" group.
        Group managerGroup = groupService.findByName(context, managerGroupName);
        if (person.getGroups().stream().noneMatch(group -> group.equals(managerGroup))) {
            return false;
        }
        // Determine if the person is manager for some degree code.
        return hasManagedDegrees(context, person);
    }

    @Override
    public boolean isManagerFor(Context context, EPerson person, String degreeCode) {
        try {
            return getManagedDegree(context, person)
                    .stream()
                    .map(d -> d.replaceAll("\\*", ".*")) // Convert '*' to correct regexp '.*' char sequence
                    .anyMatch(regex -> Pattern.matches(regex, degreeCode));
        } catch (SQLException | AuthorizeException ex) {
            log.warn(String.format(
                    "Unable to determine person is manager for [%s]-[%s]:: %s",
                    person.getID(), degreeCode, ex.getMessage())
            );
        }
        return false;
    }

    @Override
    public List<String> getManagedDegree(Context context, EPerson person) throws SQLException, AuthorizeException {
        ResearcherProfile rp = researcherProfileService.findById(context, person.getID());
        if (rp == null) {
            return new ArrayList<>();
        }
        return rp.getDegreeCodes();
    }

    private boolean hasManagedDegrees(Context context, EPerson person) {
        try {
            return !getManagedDegree(context, person).isEmpty();
        } catch (SQLException | AuthorizeException e) {
            log.warn("Unable to determine if person has managed degree :: " + person.getID());
            return false;
        }
    }
}
