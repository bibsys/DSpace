/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.consumer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.core.Utils;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.factory.SubmissionServiceFactory;
import org.dspace.submit.service.SubmissionConfigService;
import org.dspace.uclouvain.core.utils.ItemUtils;

/**
 * Consumer to remove the 'filler' values from an item's metadata.
 * Filler values are values added by the user which are there just to fill the field.
 * For example: NA, N/A , non-applicable or not specified
 * 
 * This consumer aims to clean those fields by:
 *  - Deleting them (for most fields)
 * or
 *  - Replacing them with a placeholder (for fields in a form group)
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class FillerValueRemoverConsumer implements Consumer {

    private ItemService itemService;
    private SubmissionConfigService submissionConfigService;
    private DCInputsReader dciReader;

    // List of string values to clean from the item values.
    private List<String> valuesToClear;
    private Set<UUID> itemToProcess;
    private Logger logger;

    /** Initialize the necessary resource for this consumer */
    @Override
    public void initialize() throws Exception {
        // Get the list of regexp filler values to clean from the configuration.
        valuesToClear = Arrays.asList(
            DSpaceServicesFactory
                .getInstance()
                .getConfigurationService()
                .getArrayProperty("event.consumer.fillervalueremover.fillers", new String[0])
        );
        itemService = ContentServiceFactory.getInstance().getItemService();
        submissionConfigService = SubmissionServiceFactory.getInstance().getSubmissionConfigService();
        dciReader = new DCInputsReader();
        itemToProcess = new HashSet<>();
        logger = LogManager.getLogger(FillerValueRemoverConsumer.class);
    }

    /**
     * Check if the given event subject can be processed by this consumer.
     * 
     * @param context The current DSpace context.
     * @param event The event to handle in the consumer.
     * @throws Exception
     */
    @Override
    public void consume(Context context, Event event) throws Exception {
        // Check that item can be processed (in archive).
        Item item = (Item) event.getSubject(context);
        if (item != null && !ItemUtils.isWorkspace(context, item)) {
            itemToProcess.add(item.getID());
        }
    }

    /**
     * For each item to process:
     *  1. Get the full form configuration for the item using its collection.
     *  2. For the all the fields in the form, if it has a 'filler' value:
     *      - If its in a 'form-group', replace the value by CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE.
     *      - If its a regular form field, just delete the value.
     * 
     * @param context The current DSpace context.
     * @throws Exception
     */
    @Override
    public void end(Context context) throws Exception {
        for (UUID uuid: itemToProcess) {
            try {
                Item item = itemService.find(context, uuid);
                Collection linkedCollection = ItemUtils.getMainCollection(context, item);
                if (linkedCollection == null) {
                    logger.warn("Found no collection for given item: [" + item.getID() + "]");
                    continue;
                }

                SubmissionConfig config = submissionConfigService.getSubmissionConfigByCollection(linkedCollection);
                if (config == null) {
                    logger.warn(
                        "Found no submission configuration for collection: [" + linkedCollection.getID() + "]"
                    );
                    continue;
                }

                List<String> formGroupFields = new ArrayList<>();
                for (int i = 0; i < config.getNumberOfSteps(); i++) {
                    SubmissionStepConfig stepConfig = config.getStep(i);
                    // Process only submission forms
                    if (stepConfig.getType().equals(SubmissionStepConfig.INPUT_FORM_STEP_NAME)) {
                        DCInputSet inputSet = dciReader.getInputsByFormName(stepConfig.getId());
                        // Extract only fields that are from a formGroup.
                        extractFormGroupFields(stepConfig, inputSet, formGroupFields, dciReader, false);
                    }
                }

                // Metadata to remove.
                List<MetadataValue> mvsToClear = new ArrayList<>();
                // Metadata to replace with a placeholder.
                List<MetadataValue> mvsToReplace = new ArrayList<>();

                item.getMetadata().stream()
                    .filter(metadata -> {
                        String valueToCheck = metadata.getValue();
                        return valueToCheck.isBlank() || valuesToClear.stream()
                            .anyMatch(regex -> valueToCheck.matches(regex));
                    })
                    .forEach(md -> {
                        if (formGroupFields.contains(md.getMetadataField().toString('.'))) {
                            mvsToReplace.add(md);
                        } else {
                            mvsToClear.add(md);
                        }
                    });

                if (!mvsToClear.isEmpty()) {
                    try {
                        itemService.removeMetadataValues(
                            context,
                            item,
                            mvsToClear
                        );
                    } catch (SQLException e) {
                        logger.warn("Could not remove unwanted metadata values from item with id: " + uuid, e);
                    }
                }

                for (MetadataValue mv: mvsToReplace) {
                    try {
                        // Replace the given metadata with a placeholder.
                        itemService.replaceMetadata(
                            context,
                            item,
                            mv.getSchema(),
                            mv.getElement(),
                            mv.getQualifier(),
                            mv.getLanguage(),
                            CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE,
                            mv.getAuthority(),
                            mv.getConfidence(),
                            mv.getPlace()
                        );
                    } catch (SQLException e) {
                        logger.warn(
                            "Could not replace with placeholder for metadata "
                            + mv.getMetadataField() + " of item: " + uuid,
                            e
                        );
                    }
                }

                if (!(mvsToClear.isEmpty() && mvsToReplace.isEmpty())) {
                    itemService.update(context, item);
                }
            } catch (Exception e) {
                logger.error("An error occurred while cleaning filler values for item: [" + uuid + "]", e);
            }
        }
        itemToProcess.clear();
    }

    /**
     * Given a specific inputSet, extract all DCInput that are in a form group in the given list.
     * 
     * @param stepConfig The current submission step.
     * @param inputSet The set containing all the DCInput.
     * @param groupFields The list to fill with DCInput.
     * @param dciReader The reader used to get the inputs that are inside a given form group.
     * @param isFormGroup A flag to indicate if we are processing a form group.
     * @throws DCInputsReaderException If we could not get the desired form group.
     */
    private void extractFormGroupFields(
        SubmissionStepConfig stepConfig,
        DCInputSet inputSet,
        List<String> groupFields,
        DCInputsReader dciReader,
        boolean isFormGroup
    ) throws DCInputsReaderException {
        for (DCInput[] fields: inputSet.getFields()) {
            for (DCInput input: fields) {
                // If the input has a 'group-like' type, search for 'children' fields and process them.
                if (input.getInputType() != null && StringUtils.equalsAny(input.getInputType().toLowerCase(),
                    "group", "inline-group", "inline-labeled-group")) {
                    // Get the form corresponding to the group field.
                    DCInputSet inputSetGroup = dciReader.getInputsByFormName(stepConfig.getId() + "-"
                        + Utils.standardize(input.getSchema(), input.getElement(), input.getQualifier(), "-"));
                    // Extract the fields in the map by putting the 'inGroup' flag to true.
                    extractFormGroupFields(stepConfig, inputSetGroup, groupFields, dciReader, true);
                    // Skip below code because we dont want to process the current input if it is of type group.
                    continue;
                }

                if (isFormGroup) {
                    groupFields.add(input.getFieldName());
                }
            }
        }
    }

    @Override
    public void finish(Context context) throws Exception {}
}
