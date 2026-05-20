/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.async;

import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.indexobject.factory.IndexObjectFactoryFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.services.UCLouvainProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

/**
 * Service to asynchronously re-index all publication of a profile when an eperson is linked to it.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@Service
@EnableAsync
public class PublicationProfileAsyncIndexingService {

    private static final Logger logger = LogManager.getLogger(PublicationProfileAsyncIndexingService.class);

    @Autowired
    private ItemService itemService;
    @Autowired
    private UCLouvainProfileService profileService;

    private IndexingService indexer = DSpaceServicesFactory
        .getInstance().getServiceManager()
        .getServiceByName(IndexingService.class.getName(), IndexingService.class);
    private IndexObjectFactoryFactory indexObjectServiceFactory = IndexObjectFactoryFactory.getInstance();

    @Async
    public void indexPublicationsForProfile(UUID profileUUID) {
        try (Context context = new Context()) {
            context.turnOffAuthorisationSystem();
            Item profile = itemService.find(context, profileUUID);
            if (profile == null) {
                logger.warn("Profile item not found for UUID: {}, aborting async index.", profileUUID);
                return;
            }
            for (Item publication : getPublicationsToIndex(context, profile)) {
                try {
                    List<IndexableObject> objects = indexObjectServiceFactory.getIndexableObjects(context, publication);
                    if (objects != null && !objects.isEmpty()) {
                        IndexableObject io = objects.get(0);
                        indexer.indexContent(context, io);
                        logger.info("Re-indexed publication {} due to profile link", publication.getID());
                    } else {
                        logger.warn("No indexable object found for publication {}", publication.getID());
                    }
                } catch (Exception e) {
                    logger.error("Failed to index content for publication: {}", publication.getID(), e);
                }
            }
            context.complete();
        } catch (Exception e) {
            logger.error("Could not index publication of profile " + profileUUID.toString() + " for eperson link", e);
        }
    }

    private List<Item> getPublicationsToIndex(Context context, Item profile) {
        return profileService.findLinkedPublications(context, profile);
    }
}
