/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.cleanMetadata;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
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
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.submit.service.SubmissionConfigService;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.springframework.beans.factory.annotation.Autowired;

public class CleanMetadataServiceImpl implements CleanMetadataService {

    private final Logger log = LogManager.getLogger(CleanMetadataServiceImpl.class);

    @Autowired
    protected ItemService itemService;
    @Autowired
    protected SubmissionConfigService submissionConfigService;

    private DCInputsReader dciReader;

    @PostConstruct
    void init() throws Exception {
        this.dciReader = new DCInputsReader();
    }

    /**
     * This method checks the item's metadata and cleans up the ones that should be hidden by type-bind.
     *
     * @param context the DSpace application context
     * @param item the {@link Item} to clean
     * @throws AuthorizeException if any authorization exception occurred
     * @throws SQLException if any database exception occurred
     */
    @Override
    public void cleanMetadata(Context context, Item item, boolean autoUpdate) throws AuthorizeException, SQLException {
        // Retrieve field and field value corresponding to type-bind values to analyze further.
        // This map should contain something like {"dc.type":"myItemType", 'dc.type.mainType':"text::book"}
        Map<String, String> typeBindsValues = getTypeBindValues(item);

        // Collects all "type-binded" fields specified into the submission form corresponding to the item
        Map<String, List<DCInput>> submissionFormTypeBindFields = getTypeBindFields(context, item);

        // List that holds all the invalid fields for the current type.
        List<String> invalidInputs = extractNonValidFieldsFromMap(typeBindsValues, submissionFormTypeBindFields);
        // Once we have the complete accepted metadata list, we check the ones of the item.
        List<MetadataValue> metadataToRemove =  item.getMetadata().stream()
            .filter(mv -> invalidInputs.contains(mv.getMetadataField().toString('.')))
            .toList();
        if (!metadataToRemove.isEmpty()) {
            log.debug("Found {} metadata to remove because type-bind was not valid: {}",
                metadataToRemove.size(),
                metadataToRemove.stream().map(MetadataValue::getValue).collect(Collectors.joining(","))
            );
            context.turnOffAuthorisationSystem();
            itemService.removeMetadataValues(context, item, metadataToRemove);
            if (autoUpdate) {
                itemService.update(context, item);
            }
            context.restoreAuthSystemState();
        }
    }

    /**
     * Retrieve a HashMap containing all the type-bind fields and their values for the given item.
     *
     * @param item The {@link Item} to extract values of type-bind fields from.
     * @return A map containing the type-bind field and their corresponding values.
     */
    private Map<String, String> getTypeBindValues(Item item) {
        // We cannot use `TypeBindUtils.getTypeBindValues()` since we have no access to in-progress submission object.
        return TypeBindUtils
            .getTypeBindField().stream()
            .map(field -> Map.entry(
                field,
                Optional.ofNullable(itemService.getMetadataFirstValue(item, new MetadataFieldName(field), null))
            ))
            .filter(entry -> entry.getValue().isPresent())
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
    }

    /**
     * Retrieve all field that are "type-binded" for a specific item after analyze the submission form used to submit
     * this kind of item
     *
     * @param context de DSpace application context
     * @param item the {@link Item} to analyze
     * @return a map reflecting all "type-binded" possible into the submission form used by this item.
     */
    private Map<String, List<DCInput>> getTypeBindFields(Context context, Item item) {
        Map<String, List<DCInput>> typeBindFields = new HashMap<>();
        Collection linkedCollection = ItemUtils.getMainCollection(context, item);
        if (linkedCollection == null) {
            log.warn("No collection found for item {}", item);
            return typeBindFields;
        }
        SubmissionConfig config = submissionConfigService.getSubmissionConfigByCollection(linkedCollection);
        if (config == null) {
            log.warn("No submission form found for the collection \"{}\"", linkedCollection.getName());
        }
        try {
            for (int i = 0; i < config.getNumberOfSteps(); i++) {
                SubmissionStepConfig stepConfig = config.getStep(i);
                // Process only submission forms
                if (stepConfig.getType().equals(SubmissionStepConfig.INPUT_FORM_STEP_NAME)) {
                    DCInputSet inputSet = dciReader.getInputsByFormName(stepConfig.getId());
                    extractTypeBindFields(stepConfig, inputSet, typeBindFields, dciReader);
                }
            }
        } catch (DCInputsReaderException e) {
            log.error("Error while reading submission config :: ", e);
        }
        return typeBindFields;
    }

    /**
     * Recursive method to build a map of type-bind fields.
     * Each key corresponds to a metadata field name (<schema>.<element>.<qualifier>).
     * Each value is a list of 'DCInput' for the corresponding metadata field.
     *
     * @param stepConfig The step from the submission process.
     * @param inputSet The set of inputs corresponding to a form entry (form, group, ...).
     * @param map The map to fill with type-bind fields.
     * @param inputReader A DCInput reader instance to retrieve form groups.
     * @throws DCInputsReaderException if any exception occurred during submission form analyze
     */
    private static void extractTypeBindFields(
        SubmissionStepConfig stepConfig,
        DCInputSet inputSet,
        Map<String, List<DCInput>> map,
        DCInputsReader inputReader
    ) throws DCInputsReaderException {
        for (DCInput[] fields: inputSet.getFields()) {
            for (DCInput input: fields) {
                String inputName = input.getFieldName();
                // If the input has a 'group-like' type, search for 'children' forms and process them.
                String inputType = input.getInputType().toLowerCase();
                if (StringUtils.equalsAny(inputType, "group", "inline-group", "inline-labeled-group")) {
                    DCInputSet inputSetGroup =
                        inputReader.getInputsByFormName(stepConfig.getId() + "-"
                        + Utils.standardize(input.getSchema(), input.getElement(), input.getQualifier(), "-"));
                    extractTypeBindFields(stepConfig, inputSetGroup, map, inputReader);
                    // Continue the loop here because we don't want to process the current input if it is of type group.
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
     *   - The field has a non-valid type-bind input,
     *   - For the key of a non-valid input, there are no valid input in the map.
     * For example if an entry of the map is "[key]: (nonValidDCInput, nonValidDCInput, validDCInput)", the field is
     * considered as valid since there is one valid type-bind input in the list.
     * However, if the entry looks like this: "[key]: (nonValidDCInput, nonValidDCInput)", then the field is considered
     * as non-valid and is then added to the list of non-valid field.
     *
     * @param typeBindValues The type-bind fields and values of the item being processed.
     * @param inputsByField The map of fields and corresponding DCInput.
     * @return A list of non-valid fields for the given type.
     */
    private static List<String> extractNonValidFieldsFromMap(
        Map<String, String> typeBindValues,
        Map<String, List<DCInput>> inputsByField
    ) {
        return inputsByField.entrySet().stream()
            .filter(entry -> isNotAllowed(entry.getValue(), typeBindValues))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    private static boolean isNotAllowed(List<DCInput> inputs, Map<String, String> typeBindValues) {
        return inputs.stream().noneMatch(input -> input.isAllowedFor(typeBindValues));
    }
}
