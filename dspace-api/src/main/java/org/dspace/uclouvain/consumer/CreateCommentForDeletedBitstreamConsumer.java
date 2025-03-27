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
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.uclouvain.content.service.CommentService;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;

/**
 * This consumer creates a comment on the bitstream parent item when a bitstream is deleted.
 * The user deleting the bitstream is part of the comment content.
 *
 * @version $Revision$
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class CreateCommentForDeletedBitstreamConsumer extends AbstractBitstreamConsumer implements Consumer {

    private CommentService commentService;
    private ItemService itemService;

    private final Set<Pair<UUID, UUID>> bitstreamToProcess = new HashSet<>();

    @Override
    public void initialize() throws Exception {
        super.initialize();
        commentService = UCLouvainServiceFactory.getInstance().getCommentService();
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        List<Integer> managedEventTypes = List.of(Event.DELETE, Event.REMOVE);
        if (!managedEventTypes.contains(event.getEventType()) || event.getSubjectType() != Constants.BITSTREAM) {
            return;
        }
        Item item = getItem(context, event);
        if (item != null && item.isArchived()) {
            bitstreamToProcess.add(Pair.of(event.getSubjectID(), item.getID()));
        }
    }

    @Override
    public void end(Context context) throws Exception {
        context.turnOffAuthorisationSystem();
        try {
            EPerson currentUser = context.getCurrentUser();
            for (Pair<UUID, UUID> pair : bitstreamToProcess) {
                Item item = itemService.find(context, pair.getRight());
                String commentContent = "Bitstream@" + pair.getLeft() + " :: bitstream deleted";
                commentService.create(context, item, currentUser, commentContent);
            }
        } finally {
            context.restoreAuthSystemState();
            bitstreamToProcess.clear();
        }
    }

    @Override
    public void finish(Context context) throws Exception {

    }
}
