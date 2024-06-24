package org.dspace.uclouvain.consumer;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.Entity;
import org.dspace.uclouvain.core.model.EntityType;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.UCLouvainEntityService;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Consumer to generate additional metadata from the faculty code ONLY for "catareto" collection.
 *
 * @version $Revision$
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 */
public class CataretroFacultyMetadataConsumer implements Consumer {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(CataretroFacultyMetadataConsumer.class);

    private String facultyCodeFieldName;
    private String facultyNameFieldName;

    private ItemService itemService;
    private WorkspaceItemService workspaceItemService;
    private WorkflowItemService<?> workflowItemService;
    private CollectionService collectionService;
    private MetadataFieldService metadataFieldService;
    private UCLouvainEntityService uclouvainEntityService;

    @Override
    public void initialize() {
        itemService = ContentServiceFactory.getInstance().getItemService();
        workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();
        workflowItemService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
        collectionService = ContentServiceFactory.getInstance().getCollectionService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        uclouvainEntityService = UCLouvainServiceFactory.getInstance().getEntityService();

        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        facultyCodeFieldName = configurationService.getProperty("uclouvain.global.metadata.facultycode.field", "masterthesis.faculty.code");
        facultyNameFieldName = configurationService.getProperty("uclouvain.global.metadata.facultyname.field", "masterthesis.faculty.name");
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (!canBeProcessed(context, event)) {
            log.debug("consume cannot be processed. Cancel consuming");
            return;
        }
        Item item = (Item) event.getSubject(context);
        MetadataField fcField = metadataFieldService.findByString(context, facultyCodeFieldName, '.');
        MetadataField fnField = metadataFieldService.findByString(context, facultyNameFieldName, '.');

        // 1) Clear all previously stored faculty names into the object.
        itemService.clearMetadata(context, item, fnField.getMetadataSchema().getName(), fnField.getElement(), fnField.getQualifier(), null);

        // 2) Retrieve faculty entities corresponding to faculty codes stored into the item.
        //    For each entity found, store the faculty entity name into the item.
        List<MetadataValue> dbFacultyCodes = itemService.getMetadataByMetadataString(item, fcField.toString('.'));
        for (MetadataValue facultyCode: dbFacultyCodes) {
            Entity entityFac = uclouvainEntityService.findFirst(facultyCode.getValue(), EntityType.FACULTY);
            if (entityFac != null) {
                itemService.addMetadata(context, item, fnField, null, entityFac.getName());
            } else {
                log.warn("Unable to retrieve faculty entity related to '" + facultyCode.getValue() + "'");
            }
        }
    }

    @Override
    public void end(Context context) throws Exception {}

    @Override
    public void finish(Context context) throws Exception {}

    /**
     * Check if an event should be processed by this consumer.
     * 
     * @param context: The current DSpace context.
     * @param event: The event to evaluate.
     * @return True if the event is relevant for this consumer, False otherwise
     */
    private Boolean canBeProcessed(Context context, Event event) throws SQLException {
        if (event.getSubjectType() != Constants.ITEM) {
            log.warn("CataretroMetadataConsumer should not have been given this kind of subject in an event, skipping: " + event);
            return false;
        }
        Item item = (Item)event.getSubject(context);
        return item != null && isRelevantCollection(context, item) && isRelevantMetadataModified(event.getDetail());
    }
    /** Check if one modified metadata match faculty code metadata field */

    private Boolean isRelevantMetadataModified(String modifiedMetadataFields) {
        // If the modified fields list is null or empty, it could be because we delete the last "faculty code"
        // In this case, we need to execute this consumer to delete old faculty names derived from previously
        // encoded faculty codes.
        if (modifiedMetadataFields == null || modifiedMetadataFields.trim().isEmpty())
            return true;
        // Otherwise, check that any of the modified fields is a faculty code.
        return Arrays.stream(modifiedMetadataFields.split(","))
                .map(String::trim)
                .map(m -> m.replaceAll("_", "."))
                .anyMatch(x -> x.equals(this.facultyCodeFieldName));
    }
    /** Check if the modified item is a member of a "Cataretro" collection */

    private Boolean isRelevantCollection(Context context, Item item) {
        return getItemCollection(context, item)
                .stream()
                .anyMatch(c -> {
                    List<MetadataValue> metadata = collectionService.getMetadataByMetadataString(c, "dcterms.provenance");
                    return metadata.stream().map(MetadataValue::getValue).anyMatch(v -> v.equals("cataretro"));
                });
    }

    /**
     * Get collections related to an item.
     * Depending on item life-cycle, the item collection should be retrieved differently.
     *
     * @param context: The DSpace context
     * @param item: the DSpace Item to analyze.
     * @return the list of collection to which the item belongs.
     */
    private List<Collection> getItemCollection(Context context, Item item) {
        try {
            WorkspaceItem wsItem = workspaceItemService.findByItem(context, item);
            if (wsItem != null) {
                return Collections.singletonList(wsItem.getCollection());
            }
            XmlWorkflowItem wfItem = (XmlWorkflowItem) workflowItemService.findByItem(context, item);
            if (wfItem != null) {
                return Collections.singletonList(wfItem.getCollection());
            }
            return item.getCollections();
        } catch (SQLException sqe) {
            log.warn("Unable to identify item collection : " + sqe.getMessage());
            return new ArrayList<>();
        }
    }
}
