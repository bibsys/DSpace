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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.util.TypeBindUtils;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.submit.factory.SubmissionServiceFactory;
import org.dspace.submit.service.SubmissionConfigService;
import org.dspace.uclouvain.core.utils.ItemUtils;

/**
 * Consumer to clean fields that should not exist for a given type-bind type.
 * This is determined by looking at the 'dc.type' of the item and by checking the current form configuration.
 * Typically a form field that has a 'type-bind' linking to something else than the current DSpace
 * item type should be removed. This is done to avoid having useless values for the selected type.
 * 
 * Mainly used to clear unwanted data coming from Grobe plugin (aka grobid).
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class FormFieldCleanerConsumer implements Consumer {

    DCInputsReader dciReader;
    ItemService itemService;
    SubmissionConfigService submissionConfigService;
    Set<UUID> itemToProcess;

    private Logger logger = LogManager.getLogger(FormFieldCleanerConsumer.class);

    @Override
    public void initialize() throws Exception {
        dciReader = new DCInputsReader();
        itemService = ContentServiceFactory.getInstance().getItemService();
        submissionConfigService = SubmissionServiceFactory.getInstance().getSubmissionConfigService();
        itemToProcess = new HashSet<UUID>();
    }

    /**
     * Check if the item of the event is valid.
     * An item is valid if it has a value for the 'typeBindField'.
     * When an item is valid, we add its uuid to the set of item to process.
     * 
     * @param context The current DSpace context.
     * @param event The event to process.
     */
    @Override
    public void consume(Context context, Event event) throws SQLException {
        Item item = (Item) event.getSubject(context);
        if (item == null) {
            return;
        }
        itemToProcess.add(item.getID());
    }

    /**
     * Process all valid items.
     * Get the collection of the workspace item in order to retrieve the correct submission form.
     * Build a list of all non-valid type-bind controlled field.
     * If the item has a metadata for one of those field: delete it.
     * 
     * @param context The current DSpace context.
     */
    @Override
    public void end(Context context) throws Exception {
        for (UUID itemID: itemToProcess) {
            try {
                Item item = itemService.find(context, itemID);

                // Retrieve main type-bind fields and values for this item.
                HashMap<String, String> typeBindsValues = getTypeBindValues(item);

                // Map that collects all fields that have a type-bind configuration.
                HashMap<String, List<DCInput>> typeBindFields = new HashMap<>();

                // Retrieve the correct collection depending on the state of the item.
                Collection linkedCollection = ItemUtils.getMainCollection(context, item);

                if (linkedCollection == null) {
                    logger.warn("Cannot process item: couldn't find a valid collection for item: " + itemID);
                    continue;
                }

                // Load field configuration and transform it to a map.
                SubmissionConfig config = submissionConfigService.getSubmissionConfigByCollection(linkedCollection);
                for (int i = 0; i < config.getNumberOfSteps(); i++) {
                    SubmissionStepConfig stepConfig = config.getStep(i);
                    // Process only submission forms
                    if (stepConfig.getType().equals(SubmissionStepConfig.INPUT_FORM_STEP_NAME)) {
                        DCInputSet inputSet = dciReader.getInputsByFormName(stepConfig.getId());
                        extractTypeBindFields(stepConfig, inputSet, typeBindFields, dciReader);
                    }
                }
                // List that holds all the invalid fields for the current type.
                List<String> invalidTypeBindInputs = extractNonValidFieldsFromMap(typeBindsValues, typeBindFields);
                // Once we have the complete accepted metadata list, we check the ones of the item.
                List<MetadataValue> metadataToRemove =  item.getMetadata().stream()
                    .filter(mv -> invalidTypeBindInputs.contains(mv.getMetadataField().toString('.')))
                    .collect(Collectors.toList());

                // Delete all metadata that should not be present
                if (metadataToRemove.size() != 0) {
                    context.turnOffAuthorisationSystem();
                    try {
                        this.logger.debug(
                            "Found metadata to remove because type-bind was not valid: " + metadataToRemove
                        );
                        itemService.removeMetadataValues(context, item, metadataToRemove);
                        itemService.update(context, item);
                    } finally {
                        context.restoreAuthSystemState();
                    }
                }
            } catch (Exception e) {
                logger.error(
                    "An error occurred while trying to clear unwanted type-bind data of item: " + itemID.toString(), e
                );
            }
        }
        itemToProcess.clear();
    }

    /**
     * Retrieve a HashMap containing all the type-bind fields and their values for the given item.
     *
     * @param item The item to extract values of type -bind fields from.
     * @return A Map containing the type-bind field and their corresponding values.
     */
    private HashMap<String, String> getTypeBindValues(Item item) {
        // We cannot use `TypeBindUtils.getTypeBindValues()` since we have no access to in-progress submission object.
        HashMap<String, String> typeBindsValues = new HashMap<>();
        TypeBindUtils.getTypeBindField().stream().forEach((typeBindField) -> {
            String md = itemService.getMetadataFirstValue(item, new MetadataFieldName(typeBindField), null);
            if (md != null) {
                typeBindsValues.put(typeBindField, md);
            }
        });
        return typeBindsValues;
    }

    /**
     * Build the HashMap of type-bind fields.
     * Each key corresponds to a metadata field name (<schema>.<element>.<qualifier>).
     * Each value is a list of 'DCInput' for the corresponding metadata field.
     * 
     * @param stepConfig The step from the submission process.
     * @param inputSet The set of inputs corresponding to a form.
     * @param map The HashMap to fill with type-bind fields.
     * @param inputReader A DCInput reader instance to retrieve form groups.
     * @throws DCInputsReaderException
     */
    private static void extractTypeBindFields(
        SubmissionStepConfig stepConfig,
        DCInputSet inputSet,
        HashMap<String, List<DCInput>> map,
        DCInputsReader inputReader
    ) throws DCInputsReaderException {
        for (DCInput[] fields: inputSet.getFields()) {
            for (DCInput input: fields) {
                String inputName = input.getFieldName();

                // If the input has a 'group-like' type, search for 'children' forms and process them.
                if (StringUtils.equalsAny(input.getInputType().toLowerCase(),
                    "group", "inline-group", "inline-labeled-group")) {
                    DCInputSet inputSetGroup = inputReader.getInputsByFormName(stepConfig.getId() + "-"
                        + Utils.standardize(input.getSchema(), input.getElement(), input.getQualifier(), "-"));
                    extractTypeBindFields(stepConfig, inputSetGroup, map, inputReader);
                    // Continue the loop here because we dont want to process the current input if it is of type group.
                    continue;
                }

                // If the input has type-bind information, add it to the map.
                if (input.getTypeBindMap() != null && !input.getTypeBindMap().isEmpty()) {
                    map.computeIfAbsent(inputName, key -> new ArrayList<>()).add(input);
                }
            }
        }
    }

    /**
     * From a map of type-bind metadata fields, finds out which metadata field is not valid for the current type.
     * A field is not valid if:
     * -> The field has a non-valid type-bind input,
     * -> For the key of a non-valid input, there are no valid input in the map.
     * 
     * For example if an entry of the map is "[key]: (nonValidDCInput, nonValidDCInput, validDCInput)",
     * the field is considered as valid since there is one valid type-bind input in the list.
     * However, if the entry looks like this: "[key]: (nonValidDCInput, nonValidDCInput)",
     * then the field is considered as non-valid and is then added to the list of non valid field.
     * 
     * @param typeBindValues The type-bind fields and values of the item being processed.
     * @param map The map of fields and corresponding DCInput.
     * @return A list of non-valid fields for the given type.
     */
    private static List<String> extractNonValidFieldsFromMap(
        HashMap<String, String> typeBindValues,
        HashMap<String, List<DCInput>> map
    ) {
        return map.keySet().stream().filter(key -> {
            return !map.get(key).stream().anyMatch(input -> input.isAllowedFor(typeBindValues));
        }).collect(Collectors.toList());
    }

    @Override
    public void finish(Context context) throws Exception {}
}
