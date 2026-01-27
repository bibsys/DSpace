/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import static org.dspace.content.authority.Choices.CF_UNSET;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationAuthor;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer to add a default author role for each author of a publication.
 * Every time a publication object is modified, we check every author role.
 * If a role is null or has a placeholder value we set it to 'author'.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class DefaultAuthorRoleConsumer implements Consumer {

    protected ItemService itemService;
    protected Set<UUID> itemToProcess = new HashSet<>();

    protected static final Logger logger = LoggerFactory.getLogger(DefaultAuthorRoleConsumer.class);

    @Override
    public void initialize() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (event.getSubjectType() != Constants.ITEM) {
            return;
        }
        Item item = (Item) event.getSubject(context);
        if (item != null && itemService.getEntityType(item).equals(Publication.ENTITY_TYPE)) {
            itemToProcess.add(item.getID());
        }
    }

    @Override
    public void end(Context context) throws Exception {
        if (itemToProcess.isEmpty()) {
            return;
        }
        Set<Item> itemsToUpdate = new HashSet<>();
        for (UUID itemUUID : itemToProcess) {
            Item item = itemService.find(context, itemUUID);
            if (item == null) {
                continue;
            }
            try {
                Publication publication = PublicationFactory.build(item);
                for (PublicationAuthor author : publication.getAuthors()) {
                    if (!isRoleValid(author.getRole())) {
                        // Change author role to default for this exact place.
                        itemService.setMetadataInPlace(
                            context,
                            item,
                            Publication.AUTHOR_ROLE_FIELD,
                            null,
                            PublicationAuthor.ROLE_AUTHOR,
                            null,
                            author.getPlace(),
                            CF_UNSET);
                        itemsToUpdate.add(item);
                    }
                }
            } catch (Exception e) {
                logger.warn(String.format(
                        "Error occurred when trying to check for default author role of publication with id '%s'",
                        itemUUID.toString()), e);
                continue;
            }
        }
        for (Item itemToUpdate : itemsToUpdate) {
            // Persist changes if the item has changed.
            itemService.update(context, itemToUpdate);
        }
        itemsToUpdate.clear();
        itemToProcess.clear();
    }

    @Override
    public void finish(Context context) throws Exception {
    }

    // PRIVATE METHODS -------------------------------------------------------------------------------------------------

    /**
     * Check if a role has a valid value.
     * @param role An author role.
     * @return True if the role is not null and has a value which differs from the placeholder value.
     */
    private boolean isRoleValid(String role) {
        return role != null && !role.equals(PLACEHOLDER_PARENT_METADATA_VALUE);
    }
}
