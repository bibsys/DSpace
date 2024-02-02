package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.Iterator;

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

import com.google.common.collect.Iterators;


/**
 * This authorization feature is a bit different from 'canSubmit'.
 * In this feature we rather check if the user can make many submissions, only one or none.
 * This is useful in cases where a student can only make one submission (for a master thesis for ex.)
 * but where an administrator account can submit multiple time.
 */
@Component
@AuthorizationFeatureDocumentation(name = CanCreateSubmission.NAME, 
    description = "It can be used to verify if a user has the rights to create a new submission in the repository.")
public class CanCreateSubmission implements AuthorizationFeature{
    public static final String NAME = "canCreateSubmission";

    @Autowired
    AuthorizeService authService;

    @Autowired
    ItemService itemService;

    @Autowired
    ConfigurationService configurationService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        // TODO: In future, add a config for each item type (thesis, PR...) 
        return this.isUserAuthorized(context);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            CollectionRest.CATEGORY + "." + CollectionRest.NAME,
            SiteRest.CATEGORY + "." + SiteRest.NAME
        };
    }

    /**
     * Check if the user can make a new submission based on his roles.
     * 
     * @param ctx: The current DSpace context.
     * @return: True if the user can submit, false if not :).
    */
    private Boolean isUserAuthorized(Context ctx) throws SQLException {
        EPerson currentUser = ctx.getCurrentUser();
        System.out.println(currentUser.getGroups());
        if (currentUser != null) {
            for (Group group: currentUser.getGroups()){
                String groupName = group.getName();
                // Two different cases to accept the submission creation:
                // 1. - The user is an administrator, manager... 
                // 2. - The user is a student which has not already submitted something
                System.out.println(this.canSubmitMultipleTimes(groupName));
                System.out.println(this.canSubmitOnce(groupName));
                if (this.canSubmitMultipleTimes(groupName) || (this.canSubmitOnce(groupName) && this.numberOfSubmissionForEperson(ctx, currentUser) < 1)) {
                    // NOTE: I don't think this can be simplified because we don't need to return false if the condition is not true in order to continue in the for loop. :)
                    return true;
                };
            }
        }
        return false;
    }

    /**
     * Check if the user can make many submissions.
     * 
     * @param searchedValue: A role (a string) of a user.
     * @return: True if the user can submit multiple times, false if not.
    */
    private boolean canSubmitMultipleTimes(String searchedValue) {
        return this.isPresentForProperty("uclouvain.feature.can_create_submission.permit_all_time", searchedValue);
    }

    /**
     * Check if the user can make one submission.
     * 
     * @param searchedValue: A role (a string) of a user.
     * @return: True if the user can submit one time, false if not.
    */
    private boolean canSubmitOnce(String searchedValue) {
        return this.isPresentForProperty("uclouvain.feature.can_create_submission.permit_once", searchedValue);
    }

    /**
     * Little util method that checks if a string value is present in a list of values coming from a DSpace config.
     * 
     * @param configurationPropertyName: A configuration property name to check values from.
     * @param searchedValue: The value we are searching for.
     * @return: true if searchedValue equals to one of the value of configurationPropertyName, false otherwise.
    */
    private boolean isPresentForProperty(String configurationPropertyName, String searchedValue) {
        System.out.println(configurationPropertyName);
        for (String string: this.configurationService.getArrayProperty(configurationPropertyName)){
            System.out.println(searchedValue);
            System.out.println(string);
            if (string.equals(searchedValue)) return true;
        };
        return false;
    }

    /**
     * Give the number of submission made by a given eperson.
     * 
     * @param ctx: The current DSpace context.
     * @param eperson: The eperson to check submission for.
     * @return: The number of submission the eperson has made no matter of the state (archived, workflow, workspace)
    */
    private Integer numberOfSubmissionForEperson(Context ctx, EPerson eperson) throws SQLException {
        Iterator<Item> submissions = itemService.findBySubmitter(ctx, eperson, true);
        return Iterators.size(submissions);
    }
}
