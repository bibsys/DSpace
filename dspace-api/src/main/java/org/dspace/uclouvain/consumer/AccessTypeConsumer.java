/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.access.status.factory.AccessStatusServiceFactory;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.plugins.UCLouvainAccessStatusHelper;

/**
 * Consumer to update the access type metadata of an item.
 * This consumer is triggered by bitstream events and refreshes, if necessary, the global item's access type.
 *
 * @version $Revision$
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class AccessTypeConsumer implements Consumer {

    private AccessStatusService accessStatusService;
    private ItemService itemService;
    private BitstreamService bitstreamService;
    private MetadataField accessTypeField;
    private Logger logger;

    @Override
    public void initialize() throws Exception {
        logger = LogManager.getLogger(AccessTypeConsumer.class);
        accessStatusService = AccessStatusServiceFactory.getInstance().getAccessStatusService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        accessTypeField = new MetadataField(
                configService.getProperty("uclouvain.global.metadata.accesstype.field", "dcterms.accessRights")
        );
    }

    /**
     * Consume the event and update the access type metadata of the item.
     * Only update the metadata if the access type has changed or did not exist.
     * 
     * Note that all the events that enter this method are pre-filtered, and we only deal with bitstream events.
     * This is configured in "event.consumer.accesstype.filters" field.
     * 
     * @param context  The current DSpace context.
     * @param event    The event to consume that deals with a bitstream.
     * @throws Exception for any database exception
     */
    @Override
    public void consume(Context context, Event event) throws Exception {
        // Retrieve the item related to the event.
        Item item = getItem(context, event);
        if (item == null) {
            logger.warn("Cannot retrieve the item for event type [" + event.getEventTypeAsString() + "]");
            return;
        }

        context.turnOffAuthorisationSystem();
        try {
            consume(context, item);
        } finally {
            context.restoreAuthSystemState();
        }

    }
    private void consume(Context context, Item item) throws SQLException, AuthorizeException {
        // Calculate the item global access type based on attached bitstream
        //   1) if `accessType` is empty or UNKNOWN, remove the possible existing item metadata
        //   2) if `accessType` change from possible existing item metadata, update the item.
        String accessType = accessStatusService.getAccessStatus(context, item);
        if (StringUtils.isEmpty(accessType) || accessType.equals(UCLouvainAccessStatusHelper.UNKNOWN)) {
            itemService.clearMetadata(
                    context,
                    item,
                    accessTypeField.getSchema(),
                    accessTypeField.getElement(),
                    accessTypeField.getQualifier(),
                    null
            );
            itemService.update(context, item);
            return;
        }
        String previousMetadata = itemService.getMetadataFirstValue(item, accessTypeField, null);
        if (!accessType.equals(previousMetadata)) {
            itemService.setMetadataSingleValue(context, item, accessTypeField, null, accessType);
            itemService.update(context, item);
        }
    }

    @Override
    public void finish(Context context) {}

    @Override
    public void end(Context context) throws Exception {}

    /**
     * Retrieve the item linked to a bitstream event.
     * If the event type is 'DELETE', the item is extracted from the event object.
     * Otherwise, the item is retrieved from the bitstream parent object.
     * This is done because a deleted bitstream does not have a parent object anymore.
     * 
     * @param context The current DSpace context.
     * @param event   The event to consume.
     * @return The item linked to the bitstream event.
     * @throws SQLException if any database exception occurred
     */
    private Item getItem(Context context, Event event) throws SQLException {
        return (event.getEventType() == Event.DELETE)
            ? ((Item) event.getObject(context))
            : ((Item) bitstreamService.getParentObject(context, (Bitstream) event.getSubject(context)));
    }
}
