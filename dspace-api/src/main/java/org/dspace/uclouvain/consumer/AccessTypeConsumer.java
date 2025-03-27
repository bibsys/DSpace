/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import static org.dspace.uclouvain.constants.AccessConditions.EMBARGO;
import static org.dspace.uclouvain.constants.AccessConditions.OPEN_ACCESS;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.dspace.access.status.factory.AccessStatusServiceFactory;
import org.dspace.access.status.service.AccessStatusService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.content.service.CommentService;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.plugins.UCLouvainAccessStatusHelper;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

/**
 * Consumer to store the bitstream access type as a metadata on this {@link org.dspace.content.Bitstream}, and update
 * the related {@link org.dspace.content.Item} metadata to store the global item access type.
 * This consumer is triggered by bitstream events and refreshes, if necessary, the global item's access type.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
public class AccessTypeConsumer extends AbstractBitstreamConsumer implements Consumer {

    private AccessStatusService accessStatusService;
    private CommentService commentService;
    private ContentServiceFactory contentServiceFactory;
    private ItemService itemService;
    private MetadataField accessTypeField;
    private UCLouvainResourcePolicyService policyService;

    private final Set<UUID> itemToProcess = new HashSet<>();
    private final Set<UUID> bitstreamToProcess = new HashSet<>();

    // CONSUMERS METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    @Override
    public void initialize() throws Exception {
        super.initialize();
        accessStatusService = AccessStatusServiceFactory.getInstance().getAccessStatusService();
        commentService = UCLouvainServiceFactory.getInstance().getCommentService();
        contentServiceFactory = ContentServiceFactory.getInstance();
        itemService = ContentServiceFactory.getInstance().getItemService();
        policyService = UCLouvainServiceFactory.getInstance().getResourcePolicyService();

        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        accessTypeField = new MetadataField(
                configService.getProperty("uclouvain.global.metadata.accesstype.field", "dcterms.accessRights")
        );
    }

    /**
     * This consumer should be triggered only for a bitstream object, and if the bitstream owner is an Item.
     *
     * @param context  The current DSpace context.
     * @param event    The event to consume that deals with a bitstream.
     * @throws Exception for any database exception
     */
    @Override
    public void consume(Context context, Event event) throws Exception {
        if (event.getSubjectType() != Constants.BITSTREAM) {
            return;
        }
        Item item = getItem(context, event);
        if (item != null) {
            // We don't need to manage the case where the bitstream is deleted.
            // In this case, no need to compute and store `accessCondition` metadata.
            if (event.getEventType() != Event.DELETE && event.getEventType() != Event.REMOVE) {
                bitstreamToProcess.add(event.getSubjectID());
            }
            itemToProcess.add(item.getID());
        }
    }

    @Override
    public void finish(Context context) throws Exception {}

    @Override
    public void end(Context context) throws Exception {
        context.turnOffAuthorisationSystem();
        try {
            for (UUID id : bitstreamToProcess) {
                processBitstream(context, bitstreamService.find(context, id));
            }
            for (UUID id : itemToProcess) {
                processItem(context, itemService.find(context, id));
            }
        } finally {
            context.restoreAuthSystemState();
            bitstreamToProcess.clear();
            itemToProcess.clear();
        }
    }

    // PRIVATE METHODS ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    private void processBitstream(Context context, Bitstream bitstream) throws AuthorizeException, SQLException {
        String accessType = accessStatusService.getBitstreamAccessStatus(context, bitstream);
        boolean updated = updateAccessTypeMetadata(context, bitstream, accessType);

        // If the bitstream access type changed, we would like to add a new comment to reflect this change.
        // We only add comment if the bitstream parent object is an `Item` and this item is already archived; We will
        // create a comment only if the item is not in workspace state.
        DSpaceObject parentObj = bitstreamService.getParentObject(context, bitstream);
        if (updated && parentObj instanceof Item && !ItemUtils.isWorkspace(context, (Item) parentObj)) {
            // Compute the access status full string (for embargo, it includes the embargo end-date)
            ResourcePolicy masterPolicy = policyService.getMasterPolicy(policyService.find(context, bitstream));
            String accessStatus = (masterPolicy != null) ? masterPolicy.getRpName() : OPEN_ACCESS;
            if (masterPolicy != null && masterPolicy.getRpName().equalsIgnoreCase(EMBARGO)) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
                accessStatus += " until (" + dateFormat.format(masterPolicy.getStartDate()) + ")";
            }
            // create a new system comment
            EPerson currentUser = context.getCurrentUser();
            String commentContent = "Bitstream@" + bitstream.getID() + " with name \"" + bitstream.getName() + "\" :: "
                    + "access condition changes to " + accessStatus;
            commentService.create(context, (Item) parentObj, currentUser, commentContent);
        }
    }

    private void processItem(Context context, Item item) throws AuthorizeException, SQLException {
        String accessType = accessStatusService.getAccessStatus(context, item);
        updateAccessTypeMetadata(context, item, accessType);
    }

    /**
     * Set an access type into metadata related to a dspace object.
     *   - If `accessType` is empty or UNKNOWN, remove the possible existing metadata.
     *   - If `accessType` value is different from previously possible existing metadata, update it.
     *
     * @param context the dspace application context
     * @param dso the {@link org.dspace.content.DSpaceObject} to update.
     * @param accessType the access type to store
     * @return True if the DspaceObject metadata has been updated, false otherwise (no changes are detected)
     * @throws AuthorizeException if any authorization exception occurred (should not happen).
     * @throws SQLException if any database exception occurred.
     */
    private boolean updateAccessTypeMetadata(Context context, DSpaceObject dso, String accessType)
            throws AuthorizeException, SQLException {
        //   1) if `accessType` is empty or UNKNOWN, remove the possible existing metadata
        //   2) if `accessType` value is different from previously possible existing metadata, update it.
        DSpaceObjectService<DSpaceObject> service = contentServiceFactory.getDSpaceObjectService(dso);
        if (StringUtils.isEmpty(accessType) || accessType.equals(UCLouvainAccessStatusHelper.UNKNOWN)) {
            String existingMetadata = service.getMetadataFirstValue(
                dso,
                accessTypeField.getSchema(),
                accessTypeField.getElement(),
                accessTypeField.getQualifier(),
                Item.ANY
            );
            if (existingMetadata != null) {
                context.turnOffAuthorisationSystem();
                service.clearMetadata(
                        context,
                        dso,
                        accessTypeField.getSchema(),
                        accessTypeField.getElement(),
                        accessTypeField.getQualifier(),
                        null
                );
                service.update(context, dso);
                context.restoreAuthSystemState();
                return true;
            }
        }
        String previousMetadata = service.getMetadataFirstValue(dso, accessTypeField, null);
        if (!accessType.equals(previousMetadata)) {
            context.turnOffAuthorisationSystem();
            service.setMetadataSingleValue(context, dso, accessTypeField, null, accessType);
            service.update(context, dso);
            context.restoreAuthSystemState();
            return true;
        }
        return false;
    }
}
