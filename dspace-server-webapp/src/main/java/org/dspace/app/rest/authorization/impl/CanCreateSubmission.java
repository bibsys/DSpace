/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This authorization feature is a bit different from 'canSubmit'.
 * In this feature, we rather check if the user can make many submissions, only one or none.
 * This is useful in cases where a student can only make one submission (for a master thesis for ex.)
 * but where an administrator account can submit multiple times.
 */
@Component
@AuthorizationFeatureDocumentation(
    name = CanCreateSubmission.NAME,
    description = "It can be used to verify if a user is allowed to create a new submission in the repository."
)
public class CanCreateSubmission implements AuthorizationFeature {

    public static final String NAME = "canCreateSubmission";
    private static final Logger logger = LogManager.getLogger();

    @Autowired
    private ItemService itemService;
    @Autowired
    private GroupService groupService;
    @Autowired
    private ConfigurationService configService;

    /**
     * Check if the user can make a new submission based on his roles.
     * 
     * @param context The current DSpace context.
     * @param object the REST object to analyze
     * @return True if the user can submit, false if not :).
    */
    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return false;
        }
        return multipleSubmissionAllowed(context, currentUser) || !hasCurrentSubmission(context, currentUser);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            CollectionRest.CATEGORY + "." + CollectionRest.NAME,
            SiteRest.CATEGORY + "." + SiteRest.NAME
        };
    }

    /**
     * Check if the user can make many submissions.
     * 
     * @param person the EPerson to check
     * @return True if the user can submit multiple times, false otherwise
    */
    private boolean multipleSubmissionAllowed(Context context, EPerson person) throws SQLException {
        // Get all groups for the person
        Set<String> allPersonGroups = groupService.allMemberGroups(context, person)
            .stream()
            .map(Group::getName)
            .collect(Collectors.toSet());
        // Get groups that can have unlimited submission from configuration
        String[] properties = configService
                .getArrayProperty("uclouvain.feature.can_create_submission.permit_all_time", new String[] {});
        // Check intersection between person groups and configuration properties
        return Arrays.stream(properties).anyMatch(allPersonGroups::contains);
    }

    /**
     * Check if a person has current submission (in any collection)
     * 
     * @param context the current DSpace context.
     * @param eperson the eperson to check submission for.
     * @return True if the user has at least one pending submission, false otherwise.
    */
    private boolean hasCurrentSubmission(Context context, EPerson eperson) throws SQLException {
        boolean retour = Streams
                .stream(itemService.findBySubmitter(context, eperson, true))
                .anyMatch(i -> isInProgressSubmission(context, i));
        return retour;
    }

    private boolean isInProgressSubmission(Context context, Item item) {
        try {
            return itemService.isInProgressSubmission(context, item);
        } catch (SQLException e) {
            logger.warn("Cannot check isInProgressSubmission(" + item.getID() + "): " + e.getMessage());
            return true;
        }
    }
}