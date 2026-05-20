/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.login.impl;

import static org.apache.commons.collections4.IteratorUtils.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.dspace.content.authority.Choices.CF_ACCEPTED;

import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.login.PostLoggedInAction;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.service.EPersonService;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.uclouvain.async.PublicationProfileAsyncIndexingService;
import org.dspace.uclouvain.services.UCLouvainProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

/**
 * Implementation of {@link PostLoggedInAction} that perform an automatic claim
 * between the logged eperson and possible profiles without eperson present in
 * the system. This pairing between eperson and profile is done starting from
 * the configured metadata of the logged in user.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResearcherProfileAutomaticClaim implements PostLoggedInAction {

    private final static Logger LOGGER = LogManager.getLogger();

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private UCLouvainProfileService uclouvainProfileService;

    @Autowired
    private PublicationProfileAsyncIndexingService publicationProfileIndexingService;

    /**
     * The field of the eperson to search for.
     */
    private final String ePersonField;

    /**
     * The field of the profile item to search.
     */
    private final String profileField;

    public ResearcherProfileAutomaticClaim(String ePersonField, String profileField) {
        Assert.notNull(ePersonField, "An eperson field is required to perform automatic claim");
        Assert.notNull(profileField, "An profile field is required to perform automatic claim");
        this.ePersonField = ePersonField;
        this.profileField = profileField;
    }

    @Override
    public void loggedIn(Context context) {

        if (isBlank(researcherProfileService.getProfileType())) {
            return;
        }

        EPerson currentUser = context.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            claimProfile(context, currentUser);
        } catch (SQLException | AuthorizeException e) {
            LOGGER.error("An error occurs during the profile claim by email", e);
        }

    }

    private void claimProfile(Context context, EPerson currentUser) throws SQLException, AuthorizeException {
        UUID id = currentUser.getID();
        String fullName = currentUser.getFullName();
        if (currentUserHasAlreadyResearcherProfile(context)) {
            return;
        }
        Item item = findClaimableProfile(context, currentUser);
        if (item == null) {  // If item is null, try to create a profile using eperson data.
            try {
                LOGGER.debug("Trying to create a fresh new profile from shibboleth metadata...");
                context.turnOffAuthorisationSystem();
                item = uclouvainProfileService.createNewProfile(context, currentUser);
            } catch (NoSuchElementException nse) {
                LOGGER.warn("Automatic profile creation failed at login :: " + nse.getMessage());
                return;
            } catch (Exception e) {
                LOGGER.warn("Automatic profile creation failed at login", e);
                return;
            } finally {
                context.restoreAuthSystemState();
            }
        }
        if (item != null) {
            context.turnOffAuthorisationSystem();
            itemService.addMetadata(context, item, "dspace", "object", "owner",
                null, fullName, id.toString(), CF_ACCEPTED);
            itemService.update(context, item);
            // Commit to make sure changes are applied (required for the re-index to work properly)
            context.commit();
            // Reload current user (can be detached due to commit) for next PostLoggedInAction.
            currentUser = context.reloadEntity(currentUser);
            context.restoreAuthSystemState();

            // Trigger a re-index of all publications to add correct 'read' solr key.
            publicationProfileIndexingService.indexPublicationsForProfile(item.getID());
        }

    }

    private boolean currentUserHasAlreadyResearcherProfile(Context context) throws SQLException, AuthorizeException {
        return researcherProfileService.findById(context, context.getCurrentUser().getID()) != null;
    }

    /**
     * Find and return a matching profile for a given eperson.
     * @param context The current DSpace context.
     * @param currentUser The user to find a matching profile for.
     * @return A matching profile for the given user, null if none found.
     */
    private Item findClaimableProfile(Context context, EPerson currentUser) throws SQLException, AuthorizeException {

        String value = getValueToSearchFor(context, currentUser);
        if (StringUtils.isEmpty(value)) {
            return null;
        }

        List<Item> items = toList(itemService.findArchivedByMetadataField(context, profileField, value)).stream()
            .filter(this::hasNotOwner)
            .filter(researcherProfileService::hasProfileType)
            .collect(Collectors.toList());

        return items.size() == 1 ? items.get(0) : null;
    }

    private String getValueToSearchFor(Context context, EPerson currentUser) {
        if ("email".equals(ePersonField)) {
            return currentUser.getEmail();
        }
        return ePersonService.getMetadataFirstValue(currentUser, new MetadataFieldName(ePersonField), Item.ANY);
    }

    private boolean hasNotOwner(Item item) {
        return CollectionUtils.isEmpty(itemService.getMetadata(item, "dspace", "object", "owner", Item.ANY));
    }

}
