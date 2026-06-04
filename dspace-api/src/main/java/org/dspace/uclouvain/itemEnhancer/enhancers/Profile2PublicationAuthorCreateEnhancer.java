/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.itemEnhancer.enhancers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.uclouvain.core.model.exceptions.InvalidModelEntityTypeException;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.services.PublicationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * When a profile is created/modified, in some cases we could have publications that mention this author
 * but have no authority link.
 * The main goal of this consumer is to link those 'unlinked' authors to the profile.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class Profile2PublicationAuthorCreateEnhancer extends MetadataEnhancer<Item> {

    @Autowired
    private ResearcherProfileService researcherProfileService;
    @Autowired
    private MetadataValueService metadataValueService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private PublicationService publicationService;

    private static final Logger logger = LogManager.getLogger(Profile2PublicationAuthorCreateEnhancer.class);

    @Override
    public boolean enhance(Context context, Item profile) {
        // Process:
        // 1. Find items mentioning the updated item and not linked.
        // 2. Update the items to link them to the updated item.
        logger.info("Creation of profile, enhancing publications...");
        if (profile == null) {
            return false;
        }
        ResearcherProfile rProfile = new ResearcherProfile(profile, false);
        Map<String, String> authorsIdentifier = researcherProfileService.getAuthorsIdentifiers(rProfile);
        if (authorsIdentifier.isEmpty()) {
            logger.debug("No identifiers found for updated author " + profile.getID());
            return false;
        }
        boolean updateRequired = false;
        // Once we have the full identifiers map, check for matching publications.
        List<Pair<DSpaceObject, Integer>> matchingPublicationsPlaces = new ArrayList<>();
        try {
            matchingPublicationsPlaces = metadataValueService.findByFieldAndValue(context, authorsIdentifier, false);
        } catch (SQLException e) {
            logger.warn("Could not find identifiers of profile for publication link.", e);
            return false;
        }

        context.turnOffAuthorisationSystem();

        for (Pair<DSpaceObject, Integer> publicationPlace : matchingPublicationsPlaces) {
            DSpaceObject dso = publicationPlace.getLeft();
            int place = publicationPlace.getRight();
            try {
                // We only manage publication items
                if (!(dso instanceof Item item)
                    || !Objects.equals(itemService.getEntityType(item), Publication.ENTITY_TYPE)) {
                    logger.debug("Cannot process item for creation because it is not a publication item.");
                    continue;
                }
                logger.debug("Found publication to update!! " + item.getID()
                        + " with title " + item.getName());
                logger.debug("Update needed at place " + place);
                // Once we have the publication and the place,
                // we need to update the metadata values of the corresponding author.
                Publication publication = PublicationFactory.build(item);
                PublicationAuthor authorToProcess = publication.getAuthor(place);
                if (authorToProcess.getAuthority() != null
                    && Objects.equals(authorToProcess.getAuthority().getItemId(), rProfile.getItemId())) {
                    // Skip because the publication is already linked to the profile. (authorities matching)
                    logger.debug("Publication is already linked to a profile, aborting...");
                    continue;
                }
                String previousRole = authorToProcess.getRole();
                // Set values of the author for the found place.
                publicationService.setAuthor(
                    context,
                    publication,
                    rProfile.getName().orElse(null),
                    rProfile.getEmail().orElse(null),
                    rProfile.getOrcid().orElse(null),
                    rProfile.getFGS().orElse(null),
                    rProfile.getInstitution().orElse(null),
                    // Use previously set role.
                    previousRole,
                    profile.getID(),
                    place,
                    false
                );
                updateRequired = true;
                // We need to fire an event here to trigger the indexing consumer and update the Solr index.
                // !! TODO: See how to handle a failed update !!
                itemService.update(context, item);
            } catch (InvalidModelEntityTypeException invalidEntityTypeException) {
                logger.warn(
                    "Could not link author profile %s to item %s :: item is not a publication".formatted(
                        profile.getID(), dso.getID()
                    ), invalidEntityTypeException);
            } catch (Exception e) {
                logger.warn(
                    "Could not link author profile %s to publication %s".formatted(profile.getID(), dso.getID()),
                    e
                );
            }
        }

        context.restoreAuthSystemState();
        return updateRequired;
    }

    @Override
    public String getSupportedAction() {
        return ACTION_CREATE;
    }

    @Override
    public String getSupportedEntityType() {
        return ResearcherProfile.ENTITY_TYPE;
    }
}
