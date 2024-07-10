package org.dspace.uclouvain.consumer;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.access.status.factory.AccessStatusServiceFactory;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
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

    private ConfigurationService configurationService;
    private AccessStatusService accessStatusService;
    private ItemService itemService;
    private BitstreamService bitstreamService;
    private MetadataField accessTypeField;
    private Logger logger;

    @Override
    public void initialize() throws Exception {
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.accessStatusService = AccessStatusServiceFactory.getInstance().getAccessStatusService();
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

        // The field where the access type metadata needs to be stored
        this.accessTypeField = new MetadataField(
            this.configurationService.getProperty("uclouvain.global.metadata.accesstype.field", "dcterms.accessRights")
        );
        this.logger = LogManager.getLogger(AccessTypeConsumer.class);
    }

    /**
     * Consume the event and update the access type metadata of the item.
     * Only update the metadata if the access type has changed or did not exist.
     * 
     * Note that all the events that enters this method are pre-filtered and we only deal with bitstream events.
     * This is configured in "event.consumer.accesstype.filters" field.
     * 
     * @param context: The current DSpace context.
     * @param event: The event to consume that deals with a bitstream.
     * @throws SQLException
     */
    @Override
    public void consume(Context context, Event event) throws SQLException {
        // Check that the subject is a bitstream.
        Item item = this.getItem(context, event);
        if (item == null) {
            logger.warn("Could not retrieve the item from the bitstream with an event type of: " + event.getEventTypeAsString());
            return;
        }
        // Retrieve the global access type for the item
        String accessType = this.accessStatusService.getAccessStatus(context, item);
        if (accessType.equals(UCLouvainAccessStatusHelper.UNKNOWN)){
            // DELETE field values on the configured 'accessTypeField'.
            this.itemService.clearMetadata(context, item, accessTypeField.getSchema(), accessTypeField.getElement(), null, null);
            return;
        }
        String previousMetadata = this.itemService.getMetadataFirstValue(item, accessTypeField, null);
        if(!accessType.equals(previousMetadata)) {
            this.itemService.setMetadataSingleValue(context, item, accessTypeField, null, accessType);
        }
    }

    @Override
    public void finish(Context context) throws Exception {}

    @Override
    public void end(Context context) throws Exception {}

    /**
     * Retrieve the item linked to a bitstream event.
     * If the event type is DELETE, the item is extracted from the event object.
     * Otherwise, the item is retrieved from the bitstream parent object.
     * This is done because a deleted bitstream does not have a parent object anymore.
     * 
     * @param context: The current DSpace context.
     * @param event: The event to consume.
     * @return Item: The item linked to the bitstream event.
     * @throws SQLException
     */
    private Item getItem(Context context, Event event) throws SQLException {
        return (event.getEventType() == Event.DELETE) 
            ? ((Item) event.getObject(context))
            : ((Item) this.bitstreamService.getParentObject(context, (Bitstream) event.getSubject(context)));
    }
}
