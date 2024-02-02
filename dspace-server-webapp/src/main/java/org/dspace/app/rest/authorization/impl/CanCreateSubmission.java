package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.CollectionRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Streams;


/**
 * This authorization feature is a bit different from 'canSubmit'.
 * In this feature we rather check if the user can make many submissions, only one or none.
 * This is useful in cases where a student can only make one submission (for a master thesis for ex.)
 * but where an administrator account can submit multiple times.
 */
@Component
@AuthorizationFeatureDocumentation(name = CanCreateSubmission.NAME, 
    description = "It can be used to verify if a user is allowed to create a new submission in the repository.")
public class CanCreateSubmission implements AuthorizationFeature{
    public static final String NAME = "canCreateSubmission";

    private static final Logger logger = LogManager.getLogger();

    @Autowired
    AuthorizeService authService;

    @Autowired
    ItemService itemService;

    @Autowired
    ConfigurationService configurationService;

    /**
     * Check if the user can make a new submission based on his roles.
     * 
     * @param ctx: The current DSpace context.
     * @return: True if the user can submit, false if not :).
    */
    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        EPerson currentUser = context.getCurrentUser();
        if (currentUser != null) {
            for (Group group: currentUser.getGroups()){
                // Two different cases to accept the submission creation:
                // 1. - The user is an administrator, manager... 
                // 2. - The user is a student which has no pending submissions
                if (this.canSubmitMultipleTimes(group.getName()) || (this.numberOfCurrentSubmissionForEperson(context, currentUser) == 0)) {
                    return true;
                };
            }
        }
        return false;
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
     * @param searchedValue: A role of a user.
     * @return: True if the user can submit multiple times, false if not.
    */
    private boolean canSubmitMultipleTimes(String searchedValue) {
        return this.isPresentForProperty("uclouvain.feature.can_create_submission.permit_all_time", searchedValue);
    }

    /**
     * Checks if a string value is present in a list of values coming from a DSpace config.
     * 
     * @param configurationPropertyName: A configuration property name to check values from.
     * @param searchedValue: The value we are searching for.
     * @return: true if searchedValue equals to one of the value of configurationPropertyName, false otherwise.
    */
    private boolean isPresentForProperty(String configurationPropertyName, String searchedValue) {
        return Arrays.asList(this.configurationService.getArrayProperty(configurationPropertyName)).contains(searchedValue);
    }

    /**
     * Give the number of in progress submission made by a given eperson.
     * 
     * @param ctx: The current DSpace context.
     * @param eperson: The eperson to check submission for.
     * @return: The number of in progress submission (workspace or workflow) the eperson has made.
    */
    private Long numberOfCurrentSubmissionForEperson(Context ctx, EPerson eperson) throws SQLException {
        Iterator<Item> submissions = itemService.findBySubmitter(ctx, eperson, true);
        return Streams.stream(submissions).filter(
            (Item item) -> {
                try {
                    return itemService.isInProgressSubmission(ctx, item);
                } catch (SQLException e) {
                    logger.warn("Could not check if item is in progress: " + e.getMessage() + " :: item id: " + item.getID());
                    return true;
                }
            }
        ).count();
    }
}
