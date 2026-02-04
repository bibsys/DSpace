/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataValueService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.PublicationService;
import org.dspace.utils.DSpace;

/**
 * When a profile is created/modified, in some cases we could have publications that mention this author
 * but have no authority link.
 * The main goal of this consumer is to link those 'unlinked' authors to the profile.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class ProfileLinkConsumer implements Consumer {

    private ItemService itemService;
    private MetadataValueService metadataValueService;
    private PublicationService publicationService;
    private ResearcherProfileService researcherProfileService;

    private static final Logger logger = LogManager.getLogger(ProfileLinkConsumer.class);

    private Set<Item> itemsToUpdate = new HashSet<>();

    @Override
    public void initialize() throws Exception {
        ContentServiceFactory contentServiceFactory = ContentServiceFactory.getInstance();
        itemService = contentServiceFactory.getItemService();
        metadataValueService = contentServiceFactory.getMetadataValueService();
        publicationService = UCLouvainServiceFactory.getInstance().getPublicationService();
        researcherProfileService = new DSpace().getSingletonService(ResearcherProfileService.class);
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        Item profileItem = (Item) event.getSubject(context);
        if (profileItem != null && ResearcherProfile.ENTITY_TYPE.equals(itemService.getEntityType(profileItem))) {
            logger.debug("In consumer for profile with uuid " + profileItem.getID());
            UUID profileUUID = profileItem.getID();
            ResearcherProfile profile = new ResearcherProfile(profileItem, false);
            Map<String, String> authorsIdentifier = researcherProfileService.getAuthorsIdentifiers(profile);
            logger.debug("Profile authors identifiers: " + authorsIdentifier);
            if (authorsIdentifier.isEmpty()) {
                return;
            }

            // Once we have the full identifiers map, check for matching publications.
            List<Pair<DSpaceObject, Integer>> matchingPublicationsPlaces = metadataValueService
                    .findByFieldAndValue(context, authorsIdentifier, false);
            context.turnOffAuthorisationSystem();
            try {
                for (Pair<DSpaceObject, Integer> publicationPlace : matchingPublicationsPlaces) {
                    DSpaceObject dso = publicationPlace.getLeft();
                    int place = publicationPlace.getRight();
                    // We only manage items
                    if (!(dso instanceof Item item)) {
                        continue;
                    }
                    try {
                        if (!Objects.equals(itemService.getEntityType(item), Publication.ENTITY_TYPE)) {
                            continue;
                        }
                        logger.debug("Found publication to update!! " + item.getID()
                                + " with title " + item.getName());
                        logger.debug("Update needed at place " + place);
                        // Once we have the publication and the place,
                        // we need to update the metadata values of the corresponding author.
                        Publication publication = PublicationFactory.build(item);
                        String previousRole = publication.getAuthor(place).getRole();
                        // Set values of the author for the found place.
                        publicationService.setAuthor(
                                context,
                                publication,
                                profile.getName().orElse(null),
                                profile.getEmail().orElse(null),
                                profile.getOrcid().orElse(null),
                                profile.getFGS().orElse(null),
                                profile.getInstitution().orElse(null),
                                // Use previously set role.
                                previousRole,
                                profileUUID,
                                place);
                        itemsToUpdate.add(item);
                        // We need to fire an event here to trigger the indexing consumer and update the Solr index.
                        context.addEvent(new Event(
                            Event.MODIFY_METADATA,
                            item.getType(),
                            item.getID(),
                            null,
                            itemService.getIdentifiers(context, item)
                        ));
                    } catch (Exception e) {
                        logger.warn(
                            "Could not link author profile %s to publication %s".formatted(profileUUID, item.getID()),
                            e
                        );
                    }
                }
            } finally {
                context.restoreAuthSystemState();
            }
        }
    }

    @Override
    public void end(Context context) throws Exception {
        context.turnOffAuthorisationSystem();
        try {
            for (Item itemToUpdate : itemsToUpdate) {
                // Do external update of item since an item can be updated multiple times in the previous process.
                itemService.update(context, itemToUpdate);
            }
            itemsToUpdate.clear();
        } finally {
            context.restoreAuthSystemState();
        }
    }

    @Override
    public void finish(Context context) throws Exception {
    }

}
