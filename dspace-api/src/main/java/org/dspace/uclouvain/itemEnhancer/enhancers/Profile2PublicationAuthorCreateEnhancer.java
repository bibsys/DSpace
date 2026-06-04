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

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
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
    public boolean enhance(Context context, Item profile) throws Exception {
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

        // Each publication is processed in its own short transaction: lock, update, commit (releases the lock).
        for (Pair<DSpaceObject, Integer> publicationPlace : matchingPublicationsPlaces) {
            DSpaceObject dso = publicationPlace.getLeft();
            int place = publicationPlace.getRight();
            try {
                // We only manage publication items
                if (!(dso instanceof Item found)
                    || !Objects.equals(itemService.getEntityType(found), Publication.ENTITY_TYPE)) {
                    logger.debug("Cannot process item for creation because it is not a publication item.");
                    continue;
                }
                // Nobody can modify the publication until the commit below. It may have changed since the query
                // above: check again, on the locked (fresh) item, that the author at this place matches the profile.
                Item item = itemService.lockForUpdate(context, found);
                if (item == null || !matchesIdentifiers(item, place, authorsIdentifier)) {
                    context.commit();
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
                    context.commit();
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
                itemService.update(context, item);
                context.commit();
            } catch (PessimisticLockException | LockTimeoutException lockException) {
                // The lock could not be acquired (e.g. deadlock with a concurrent edition of the publication):
                // the profile is retried at the next cycle (the enhancement is idempotent).
                logger.warn("Could not lock publication %s, profile %s is queued again".formatted(
                    dso.getID(), profile.getID()), lockException);
                context.rollback();
                itemEnhancerService.addItemForEnhancement(context, profile.getID(), getSupportedEntityType());
                context.commit();
                break;
            } catch (InvalidModelEntityTypeException invalidEntityTypeException) {
                logger.warn(
                    "Could not link author profile %s to item %s :: item is not a publication".formatted(
                        profile.getID(), dso.getID()
                    ), invalidEntityTypeException);
                context.rollback();
            } catch (Exception e) {
                logger.warn(
                    "Could not link author profile %s to publication %s".formatted(profile.getID(), dso.getID()),
                    e
                );
                context.rollback();
            }
        }

        context.restoreAuthSystemState();
        return updateRequired;
    }

    /**
     * Check that the author at the given place of the publication still holds one of the profile identifiers,
     * not linked by authority (same condition as the 'findByFieldAndValue' query used to find the publications).
     *
     * @param item The publication.
     * @param place The place of the author in the publication.
     * @param authorsIdentifier The profile identifiers (author metadata field => value).
     * @return true if at least one identifier matches.
     */
    private boolean matchesIdentifiers(Item item, int place, Map<String, String> authorsIdentifier) {
        return authorsIdentifier.entrySet().stream().anyMatch(identifier -> {
            MetadataValue value = itemService.getMetadataByMetadataStringAndPlace(item, identifier.getKey(), place);
            return value != null && value.getAuthority() == null
                && Objects.equals(value.getValue(), identifier.getValue());
        });
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
