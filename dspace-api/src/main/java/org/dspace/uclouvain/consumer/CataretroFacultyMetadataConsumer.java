/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
 * Consumer to generate additional metadata from the faculty code ONLY for "catareto" collection.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 * @version $Revision$
 */
public class CataretroFacultyMetadataConsumer implements Consumer {

    private String facultyCodeFieldName;
    private String facultyNameFieldName;
    private final Set<UUID> itemToProcess = new HashSet<>();

    private ItemService itemService;
    private MetadataFieldService metadataFieldService;
    private UCLouvainEntityService uclouvainEntityService;

    @Override
    public void initialize() {
        itemService = ContentServiceFactory.getInstance().getItemService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        uclouvainEntityService = UCLouvainServiceFactory.getInstance().getEntityService();

        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        facultyCodeFieldName = configService.getProperty("uclouvain.global.metadata.facultycode.field",
                "masterthesis.faculty.code");
        facultyNameFieldName = configService.getProperty("uclouvain.global.metadata.facultyname.field",
                "masterthesis.faculty.name");
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        if (event.getSubjectType() == Constants.ITEM && isRelevantMetadataModified(event.getDetail())) {
            itemToProcess.add(event.getSubjectID());
        }
    }

    @Override
    public void end(Context context) throws Exception {
        MetadataField fnField = metadataFieldService.findByString(context, facultyNameFieldName, '.');
        for (UUID id : itemToProcess) {
            Item item = itemService.find(context, id);
            if (item != null) {
                Set<String> existingFacultyNames = getExistingFacultyNames(item);
                Set<String> computedFacultyNames = getComputedFacultyNames(item);
                if (!(existingFacultyNames.equals(computedFacultyNames))) {
                    // Clear previously stored faculty name metadata
                    itemService.clearMetadata(
                            context,
                            item,
                            fnField.getMetadataSchema().getName(),
                            fnField.getElement(),
                            fnField.getQualifier(),
                            null
                    );
                    // Add new computed faculty names
                    for (String facultyName : computedFacultyNames) {
                        itemService.addMetadata(context, item, fnField, null, facultyName);
                    }
                }
            }
        }
    }

    @Override
    public void finish(Context context) throws Exception {}

    /** Check if one modified metadata match faculty code metadata field */
    private Boolean isRelevantMetadataModified(String modifiedMetadataFields) {
        // If the modified fields list is null or empty, it could be because we delete the last "faculty code"
        // In this case, we need to execute this consumer to delete old faculty names derived from previously
        // encoded faculty codes.
        if (modifiedMetadataFields == null || modifiedMetadataFields.trim().isEmpty()) {
            return true;
        }
        // Otherwise, check that any of the modified fields is a faculty code.
        return Arrays.stream(modifiedMetadataFields.split(","))
                .map(String::trim)
                .map(m -> m.replaceAll("_", "."))
                .anyMatch(x -> x.equals(facultyCodeFieldName));
    }

    /** Get faculty names stored into the item
     *
     * @param item the Item to analyze
     * @return the set of stored faculty names.
     */
    private Set<String> getExistingFacultyNames(Item item) {
        return itemService
            .getMetadataByMetadataString(item, facultyNameFieldName)
            .stream().map(MetadataValue::getValue)
            .collect(Collectors.toSet());
    }

    /** Get the faculty names based on stored faculty codes
     *
     * @param item the item to analyze
     * @return the set of faculty names corresponding to faculty codes stored into the item.
     */
    private Set<String> getComputedFacultyNames(Item item) {
        return itemService.getMetadataByMetadataString(item, facultyCodeFieldName)
            .stream()
            .map(field -> uclouvainEntityService.findFirst(field.getValue(), EntityType.FACULTY))
            .filter(Objects::nonNull)
            .map(Entity::getName)
            .collect(Collectors.toSet());
    }
}
