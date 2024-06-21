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
import java.util.List;
import java.util.stream.Collectors;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.core.model.MetadataField;
import org.dspace.uclouvain.degreeMappers.DegreeMappersService;
import org.dspace.uclouvain.degreeMappers.exceptions.GeneratorProcessException;
import org.dspace.uclouvain.degreeMappers.factory.DegreeMappersServiceFactory;
import org.dspace.uclouvain.degreeMappers.model.DegreeMapper;

/**
 * Consumer to generate additional metadata from the degree code metadata field.
 * This is using the DegreeMappersService to get the additional metadata from a configuration file.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class DegreeMetadataConsumer implements Consumer {

    private MetadataField degreeCodeField;
    private MetadataField rootDegreeCodeField;
    private MetadataField rootDegreeLabelField;
    private MetadataField facultyCodeField;
    private MetadataField facultyNameField;

    private ItemService itemService;
    private DegreeMappersService degreeMappersService;
    private ConfigurationService configService;

    @Override
    public void initialize() throws Exception {
        configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        itemService = ContentServiceFactory.getInstance().getItemService();
        degreeMappersService = DegreeMappersServiceFactory.getInstance().getDegreeMappersService();

        degreeCodeField = new MetadataField(configService.getProperty(
                "uclouvain.global.metadata.degreecode.field", "masterthesis.degree.code"));
        rootDegreeCodeField = new MetadataField(configService.getProperty(
                "uclouvain.global.metadata.rootdegreecode.field", "masterthesis.rootdegree.code"));
        rootDegreeLabelField = new MetadataField(configService.getProperty(
                "uclouvain.global.metadata.rootdegreelabel.field", "masterthesis.rootdegree.label"));
        facultyCodeField = new MetadataField(configService.getProperty(
                "uclouvain.global.metadata.facultycode.field", "masterthesis.faculty.code"));
        facultyNameField = new MetadataField(configService.getProperty(
                "uclouvain.global.metadata.facultyname.field", "masterthesis.faculty.name"));
    }

    @Override
    public void consume(Context context, Event event) throws SQLException, GeneratorProcessException {
        // Check if the changed metadata contain the degree code field
        // We also need to erase the additional metadata if the degree code is null
        Item item = (Item) event.getSubject(context);
        if (item != null) {
            return;
        }
        // This condition handles the case where an author is removed and there are none remaining.
        // So if the getDetail() is null && the degree code is empty, we clear the additional metadata.
        if (event.getDetail() == null) {
            List<MetadataValue> metadata = itemService.getMetadata(
                    item,
                    degreeCodeField.getSchema(),
                    degreeCodeField.getElement(),
                    degreeCodeField.getQualifier(),
                    null
            );
            if (metadata.isEmpty()) {
                clearPreviousMetadata(context, item);
            }
        } else if (canBeProcessed(context, event)) {
            process(context, (Item) event.getSubject(context));
        }
    }

    @Override
    public void end(Context context) throws Exception {}

    @Override
    public void finish(Context context) throws Exception {}

    /**
     * Refreshes the additional degree metadata since the degree code has been modified.
     * 
     * @param context The current Dspace context.
     * @param item The item which has seen his degree code changed and will be updated.
     */
    private void process(Context context, Item item) throws GeneratorProcessException {
        try {
            // 1) Clear previous metadata values (rootDegree, faculty, ...)
            clearPreviousMetadata(context, item);
            // 2) Get information based on the distinct retrieved degree codes and the degree mappers file
            List<MetadataValue> currentDegreeCodes = itemService.getMetadata(
                item, degreeCodeField.getSchema(),
                degreeCodeField.getElement(),
                degreeCodeField.getQualifier(),
                null
            );
            List<String> distinctCodes = currentDegreeCodes
                .stream()
                .map(MetadataValue::getValue)
                .distinct()
                .collect(Collectors.toList());
            List<DegreeMapper> mappings = degreeMappersService.getDegreeMappersForDegreeCodes(distinctCodes);
            // For each mapping found, add additional info to the item
            for (DegreeMapper mapping: mappings) {
                generateAdditionalMetadataFromMapper(context, item, mapping);
            }
            // 3) Update the item with the additional metadata
            itemService.update(context, item);

        } catch (SQLException e) {
            throw new GeneratorProcessException("An error occurred while replacing degree metadata", e);
        } catch (AuthorizeException e) {
            throw new GeneratorProcessException("Not authorized to update the item", e);
        }
    }

    /**
     * Check if an event is modifying the degree code metadata field.
     * 
     * @param context The current DSpace context.
     * @param event The event to evaluate.
     * @return True if the correct field is modified by the event. False otherwise.
     */
    private Boolean canBeProcessed(Context context, Event event) {
        return Arrays
                .stream(event.getDetail().split(","))
                .map(String::trim)
                .anyMatch(x -> x.equals(degreeCodeField.getFullString("_")));
    }

    /**
     * Clear previous metadata values for the degree code, degree label, root degree code, root degree label, faculty
     * code and faculty name.
     *
     * @param context The DSpace context.
     * @param item The item to update.
     * @throws SQLException
     */
    private void clearPreviousMetadata(Context context, Item item) throws SQLException {
        itemService.clearMetadata(
            context, item,
            rootDegreeCodeField.getSchema(),
            rootDegreeCodeField.getElement(),
            rootDegreeCodeField.getQualifier(),
            null
        );
        itemService.clearMetadata(
            context, item,
            rootDegreeLabelField.getSchema(),
            rootDegreeLabelField.getElement(),
            rootDegreeLabelField.getQualifier(),
            null
        );
        itemService.clearMetadata(
            context, item,
            facultyCodeField.getSchema(),
            facultyCodeField.getElement(),
            facultyCodeField.getQualifier(),
            null
        );
        itemService.clearMetadata(
            context, item,
            facultyNameField.getSchema(),
            facultyNameField.getElement(),
            facultyNameField.getQualifier(),
            null
        );
    }

    /**
     * Generate additional metadata from a `DegreeMapper` object and add it to the item.
     *
     * @param context The DSpace context.
     * @param item The item to update.
     * @param mapping The degree mapper to use to generate the additional metadata.
     * @throws SQLException
     */
    private void generateAdditionalMetadataFromMapper(Context context, Item item, DegreeMapper mapping)
            throws SQLException {
        if (mapping.getRootDegreeCode() != null && mapping.getRootDegreeLabel() != null) {
            itemService.addMetadata(
                context, item,
                rootDegreeCodeField.getSchema(),
                rootDegreeCodeField.getElement(),
                rootDegreeCodeField.getQualifier(),
                null,
                mapping.getRootDegreeCode()
            );
            itemService.addMetadata(
                context, item,
                rootDegreeLabelField.getSchema(),
                rootDegreeLabelField.getElement(),
                rootDegreeLabelField.getQualifier(),
                null,
                mapping.getRootDegreeLabel()
            );
        }
        if (mapping.getFacultyCode() != null && mapping.getFacultyName() != null) {
            itemService.addMetadata(
                context, item,
                facultyCodeField.getSchema(),
                facultyCodeField.getElement(),
                facultyCodeField.getQualifier(),
                null,
                mapping.getFacultyCode()
            );
            itemService.addMetadata(
                context, item,
                facultyNameField.getSchema(),
                facultyNameField.getElement(),
                facultyNameField.getQualifier(),
                null,
                mapping.getFacultyName()
            );
        }
    }
}