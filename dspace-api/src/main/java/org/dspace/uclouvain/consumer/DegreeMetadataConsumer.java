package org.dspace.uclouvain.consumer;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
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

/**
 * Consumer to generate additional metadata from the degree code metadata field.
 *
 * @version $Revision$
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class DegreeMetadataConsumer implements Consumer {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DegreeMetadataConsumer.class);

    private String degreeCodeFieldName;
    private String rootDegreeCodeFieldName;
    private String rootDegreeLabelFieldName;
    private String facultyCodeFieldName;
    private String facultyNameFieldName;
    
    private ItemService itemService;
    private MetadataFieldService metadataFieldService;
    private UCLouvainEntityService uclouvainEntityService;

    @Override
    public void initialize() throws Exception {
        this.itemService = ContentServiceFactory.getInstance().getItemService();
        this.metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        this.uclouvainEntityService = UCLouvainServiceFactory.getInstance().getEntityService();

        ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.degreeCodeFieldName = configurationService.getProperty("uclouvain.global.metadata.degreecode.field", "masterthesis.degree.code");
        this.rootDegreeCodeFieldName = configurationService.getProperty("uclouvain.global.metadata.rootdegreecode.field", "masterthesis.rootdegree.code");
        this.rootDegreeLabelFieldName = configurationService.getProperty("uclouvain.global.metadata.rootdegreelabel.field", "masterthesis.rootdegree.label");
        this.facultyCodeFieldName = configurationService.getProperty("uclouvain.global.metadata.facultycode.field", "masterthesis.faculty.code");
        this.facultyNameFieldName = configurationService.getProperty("uclouvain.global.metadata.facultyname.field", "masterthesis.faculty.name");
    }

    @Override
    public void consume(Context context, Event event) throws SQLException {
        if (!canBeProcessed(context, event)) {
            log.debug("consume cannot be processed. Cancel consuming");
            return;
        }
        Item item = (Item) event.getSubject(context);
        // 1) Clear all previously stored faculty names into the object.
        this.clearPreviousMetadata(context, item);

        // 2) Retrieve entities related to degree codes stored into the item.
        //    For each entity found, store the hierarchical ancestors into the item (degree/faculty entityType only)
        Set<MetadataValue> dbDegreeCodes = new HashSet<>(itemService.getMetadataByMetadataString(item, degreeCodeFieldName));
        for(MetadataValue degreeCode : dbDegreeCodes) {
            Entity entity = uclouvainEntityService.findFirst(degreeCode.getValue(), EntityType.DEGREE);
            if (entity != null) {
                addEntityMetadata(context, item, entity.getParent());
            } else {
                log.warn("Unable to retrieve degree entity related to '" + degreeCode.getValue() + "'");
            }
        }
    }

    @Override
    public void end(Context context) throws Exception {}

    @Override
    public void finish(Context context) throws Exception {}

    /**
     * Check if an event is modifying the degree code metadata field.
     * 
     * @param context: The current DSpace context.
     * @param event  : The event to evaluate.
     * @return True if the event is relevant for this consumer, False otherwise
     */
    private Boolean canBeProcessed(Context context, Event event) throws SQLException{
        // First, we need to check the subject item exists and is an `Item`
        if (event.getSubjectType() != Constants.ITEM) {
            log.warn("DegreeMetadataConsumer should not have been given this kind of subject in an event, skipping: " + event);
            return false;
        }
        Item item = (Item)event.getSubject(context);
        if (item == null) {
            log.warn("Item cannot be found.");
            return false;
        }
        // If the modified fields list is null or empty, it could be because we delete the last "degree"
        // In this case, we need to execute this consumer to delete old metadata fields derived from
        // previously encoded degrees.
        if (event.getDetail() == null || event.getDetail().trim().isEmpty())
            return true;
        // Check event details to determine if the consumer can be processed.
        // Two cases exist:
        //   * Either the event details are `null` (when an author is removed and there are none remaining)
        //   * Either one metadata field corresponds to degree code field
        return Arrays
                .stream(event.getDetail().split(","))
                .map(String::trim)
                .map(m -> m.replace("_", "."))
                .anyMatch(x -> x.equals(this.degreeCodeFieldName));
    }

    /**
     * Clear previous metadata values stored about degree codes/labels and faculty codes/labels.
     * @param context: The DSpace context.
     * @param item: The item to update.
     * @throws SQLException if any database exception occurred
     */
    private void clearPreviousMetadata(Context context, Item item) throws SQLException {
        MetadataField rdcField = metadataFieldService.findByString(context, rootDegreeCodeFieldName, '.');
        MetadataField rdnField = metadataFieldService.findByString(context, rootDegreeLabelFieldName, '.');
        MetadataField fcField = metadataFieldService.findByString(context, facultyCodeFieldName, '.');
        MetadataField fnField = metadataFieldService.findByString(context, facultyNameFieldName, '.');

        for (MetadataField field : Arrays.asList(rdcField, rdnField, fcField, fnField)) {
            this.itemService.clearMetadata(
                context, item,
                field.getMetadataSchema().getName(),
                field.getElement(),
                field.getQualifier(),
                null
            );
        }
    }

    private void addEntityMetadata(Context context, Item item, Entity entity) throws SQLException {
        if (entity == null) {
            return;
        }
        // Determine in which metadata fields the parent entity should be mapped
        MetadataField codeField;
        MetadataField nameField;
        switch (entity.getType()) {
            case DEGREE:
                codeField = metadataFieldService.findByString(context, rootDegreeCodeFieldName, '.');
                nameField = metadataFieldService.findByString(context, rootDegreeLabelFieldName, '.');
                break;
            case FACULTY:
                codeField = metadataFieldService.findByString(context, facultyCodeFieldName, '.');
                nameField = metadataFieldService.findByString(context, facultyNameFieldName, '.');
                break;
            default:
                log.debug("Unable to manage '" + entity.getType().label + "' parent entity");
                return;
        }
        // At this time, we are sure `codeField` and `nameField` have values, so add metadata into item.
        if (entity.getCode() != null)
            itemService.addMetadata(context, item, codeField, null, entity.getCode());
        if (entity.getName() != null)
            itemService.addMetadata(context, item, nameField, null, entity.getName());
        // Recursively add potential parent ancestor entity
        addEntityMetadata(context, item, entity.getParent());
    }
}