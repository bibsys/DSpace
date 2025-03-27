/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import java.sql.SQLException;

import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;

public abstract class AbstractBitstreamConsumer implements Consumer {

    protected BitstreamService bitstreamService;

    @Override
    public void initialize() throws Exception {
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    }

    /**
     * Retrieve the item linked to a bitstream event.
     *    By default, item is the bitstream parent object.
     *    But if the event type is 'DELETE', the bitstream is already deleted; so we can't get the item calling the
     *    bitstream parent object; In this case, we can't get the parent object from the `event.getObject`.
     *    See `BitstreamServiceImpl.delete()` method for more explications.
     *
     * @param context The current DSpace context.
     * @param event   The event to consume.
     * @return The item linked to the bitstream event.
     * @throws SQLException if any database exception occurred
     */
    protected Item getItem(Context context, Event event) throws SQLException {
        DSpaceObject parent = (event.getEventType() == Event.DELETE || event.getEventType() == Event.REMOVE)
            ? event.getObject(context)
            : bitstreamService.getParentObject(context, (Bitstream) event.getSubject(context));
        return (parent instanceof Item)
            ? (Item) parent
            : null;
    }
}
