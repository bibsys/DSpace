/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
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
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
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
        this.degreeCodeFieldName = configurationService
                .getProperty("uclouvain.global.metadata.degreecode.field", "masterthesis.degree.code");
        this.rootDegreeCodeFieldName = configurationService
                .getProperty("uclouvain.global.metadata.rootdegreecode.field", "masterthesis.rootdegree.code");
        this.rootDegreeLabelFieldName = configurationService
                .getProperty("uclouvain.global.metadata.rootdegreelabel.field", "masterthesis.rootdegree.label");
        this.facultyCodeFieldName = configurationService
                .getProperty("uclouvain.global.metadata.facultycode.field", "masterthesis.faculty.code");
        this.facultyNameFieldName = configurationService
                .getProperty("uclouvain.global.metadata.facultyname.field", "masterthesis.faculty.name");
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (!canBeProcessed(context, event)) {
            return;
        }
        Item item = (Item) event.getSubject(context);

        // 1) Get existing rootDegree/faculty metadata stored into the item.
        // 2) Retrieve the rootDegree/faculty related to degrees stored into the item.
        // 3) Compare both lists; if any divergences are found, then update the item
        //    a) clear existing rootDegree/faculty metadata stored into the item
        //    b) add newly computed metadata into the item
        Set<Pair<String, String>> existingMetadata = getPreviousMetadata(item);
        Set<Pair<String, String>> computedMetadata = getRelatedEntities(item);
        if (!(existingMetadata).equals(computedMetadata)) {
            clearPreviousMetadata(context, item);
            addComputedMetadata(context, item, computedMetadata);
        }
    }

    @Override
    public void end(Context context) throws Exception {}

    @Override
    public void finish(Context context) throws Exception {}

    /**
     * Check if an event is modifying the degree code metadata field.
     * @param context the current DSpace context.
     * @param event the event to evaluate.
     * @return True if the event is relevant for this consumer, False otherwise
     */
    private Boolean canBeProcessed(Context context, Event event) throws SQLException {
        // First, we need to check the subject item exists and is an `Item`
        if (event.getSubjectType() != Constants.ITEM) {
            log.warn("Invalid subject: " + event.getSubjectType());
            return false;
        }
        Item item = (Item) event.getSubject(context);
        if (item == null) {
            log.warn("Item cannot be found.");
            return false;
        }
        // If the modified fields list is null or empty, it could be because we delete the last "degree"
        // In this case, we need to execute this consumer to delete old metadata fields derived from
        // previously encoded degrees.
        if (event.getDetail() == null || event.getDetail().trim().isEmpty()) {
            return true;
        }
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
     * Search into an `Item` to retrieve metadata related to basic degree fields
     * @param item the item to analyze
     * @return a set of "tuple" containing metadata related to basic degree fields
     */
    private Set<Pair<String, String>> getPreviousMetadata(Item item) {
        String[] fields = new String[] {
            rootDegreeCodeFieldName,
            rootDegreeLabelFieldName,
            facultyCodeFieldName,
            facultyNameFieldName
        };
        Set<Pair<String, String>> metadata = new HashSet<>();
        for (String fieldName : fields) {
            this.itemService
                .getMetadataByMetadataString(item, fieldName)
                .forEach(m -> metadata.add(Pair.of(fieldName, m.getValue())));
        }
        return metadata;
    }

    /**
     * Found entities related to an item.
     * For each degree code encoded into the items, search for degree ancestors and return them.
     * If multiple same root degrees or faculties are found, we distinct them to return a `Set`.
     * @param item the item to analyze
     * @return a set of "tuple" containing metadata related to item encoded degree code.
     */
    private Set<Pair<String, String>> getRelatedEntities(Item item) {
        return itemService.getMetadataByMetadataString(item, degreeCodeFieldName).stream()
                .map(m -> uclouvainEntityService.findFirst(m.getValue(), EntityType.DEGREE))
                .filter(Objects::nonNull)
                .flatMap(entity -> extractEntityMetadata(new HashSet<>(), entity.getParent()).stream())
                .collect(Collectors.toSet());
    }

    /** Recursive function to extract metadata about an entity and possible ancestors. */
    private Set<Pair<String, String>> extractEntityMetadata(Set<Pair<String, String>> entities, Entity entity) {
        if (entity == null) {
            return entities;
        }
        switch (entity.getType()) {
            case DEGREE:
                addIfNotNull(entities, rootDegreeCodeFieldName, entity.getCode());
                addIfNotNull(entities, rootDegreeLabelFieldName, entity.getName());
                break;
            case FACULTY:
                addIfNotNull(entities, facultyCodeFieldName, entity.getCode());
                addIfNotNull(entities, facultyNameFieldName, entity.getName());
                break;
            default:
                break;
        }
        return extractEntityMetadata(entities, entity.getParent());
    }

    private void addIfNotNull(Set<Pair<String, String>> entities, String key, String value) {
        if (value != null) {
            entities.add(Pair.of(key, value));
        }
    }

    /**
     * Clear previous metadata values stored about degree codes/labels and faculty codes/labels.
     * @param context the application context
     * @param item the item to update
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

    /**
     * Add metadata into the item
     * @param context the application context.
     * @param item the item to update
     * @param metadata metadata list to add in the item
     */
    private void addComputedMetadata(Context context, Item item, Set<Pair<String, String>> metadata) {
        metadata.forEach(pair -> {
            try {
                MetadataField mdField = metadataFieldService.findByString(context, pair.getLeft(), '.');
                itemService.addMetadata(context, item, mdField, null, pair.getRight());
            } catch (SQLException ignored) {
                log.error("Unable to add metadata :: " + pair.getLeft() + " -- " + pair.getRight());
            }
        });
    }
}