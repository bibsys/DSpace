/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.validation;

import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;
import static org.dspace.validation.util.ValidationUtils.addError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.app.util.TypeBindUtils;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.CrisConstants;
import org.dspace.validation.SubmissionStepValidator;
import org.dspace.validation.model.ValidationError;

/**
 * Checks that the value of a field declared with a list of values ({@code value-pairs-name}) in
 * {@code submission-forms.xml} is one of the stored values of that list.
 *
 * <p>The same field can be declared several times in a form, each declaration being bound to a document type
 * ({@code type-bind}) and having its own list. For example {@code dc.type.subtype} has one list for books and
 * another one for articles. Only the declarations matching the type of the item are used:</p>
 * <ul>
 *   <li>one declaration matches: the value must be in its list;</li>
 *   <li>several declarations match: the value must be in one of their lists;</li>
 *   <li>no declaration matches: the field is not checked here. Either another declaration accepts free text
 *       for this type, or the field doesn't exist for this type and
 *       {@link org.dspace.validation.MetadataValidator} removes its values.</li>
 * </ul>
 *
 * <p>The fields of a group, like the role of each author, are declared in a nested form: they are checked too,
 * value by value.</p>
 *
 * <p>The {@code qualdrop_value} fields are ignored: their list holds the qualifiers of the field, not its
 * values.</p>
 */
public class ValuePairsValidator implements SubmissionStepValidator {

    public static final String ERROR_VALIDATION_VALUE_NOT_IN_LIST = "error.validation.valueNotInList";

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private DCInputsReader inputsReader;
    private String name;

    /**
     * Validates the fields of one form of the submission.
     *
     * @param context    the DSpace context
     * @param submission the submission item to validate
     * @param stepConfig the step to validate; its ID is the name of the submission form
     * @return one error per value which is not in the list allowed for its field
     */
    @Override
    public List<ValidationError> validate(
        Context context,
        InProgressSubmission<?> submission,
        SubmissionStepConfig stepConfig
    ) {
        List<ValidationError> errors = new ArrayList<>();
        // Find the metadata values related to DSpace type-binded fields (from config) for this in-submission item.
        Map<String, String> typeBindValues = TypeBindUtils.getTypeBindValues(submission);
        // Find allowed values for the type-binded fields depending on type-binded value (from submission-forms.xml)
        Map<String, Set<String>> allowedValuesByField = getAllowedValuesByField(getForms(stepConfig), typeBindValues);

        allowedValuesByField.forEach((fieldName, allowedValues) -> {
            for (MetadataValue mdValue : itemService.getMetadataByMetadataString(submission.getItem(), fieldName)) {
                if (!isEmptyValueOfGroup(mdValue) && !allowedValues.contains(mdValue.getValue())) {
                    String path = buildPath(OPERATION_PATH_SECTIONS, stepConfig.getId(), fieldName, mdValue.getPlace());
                    addError(errors, ERROR_VALIDATION_VALUE_NOT_IN_LIST, path);
                }
            }
        });
        return errors;
    }

    /**
     * Build the fully qualified path for a field
     *
     * @param segments all path section to clean and join together
     * @return the fully qualified field path
     */
    private static String buildPath(Object... segments) {
        // Convert each segment to String and filter out null or blank values
        String joined = Arrays.stream(segments)
            .filter(Objects::nonNull)
            .map(String::valueOf)
            .filter(s -> !s.isBlank())
            .map(s -> s.replaceAll("^/+|/+$", ""))
            .collect(Collectors.joining("/"));
        return "/" + joined;
    }

    /**
     * Collects, for each field of the forms having a list of values, the values allowed for the type of the item.
     *
     * @param forms          the forms to read
     * @param typeBindValues the values of the type-bind fields of the item (document type, ...)
     * @return the allowed values by field name; a field without list for this type is absent
     */
    private Map<String, Set<String>> getAllowedValuesByField(
        List<DCInputSet> forms,
        Map<String, String> typeBindValues
    ) {
        Map<String, Set<String>> allowedValuesByField = new LinkedHashMap<>();
        for (DCInputSet form : forms) {
            for (DCInput[] row : form.getFields()) {
                for (DCInput input : row) {
                    if (hasListOfValues(input) && input.isAllowedFor(typeBindValues)) {
                        allowedValuesByField
                            .computeIfAbsent(input.getFieldName(), fieldName -> new HashSet<>())
                            .addAll(input.getAllStoredValues());
                    }
                }
            }
        }
        return allowedValuesByField;
    }

    /**
     * In a group, like the authors, a field left empty is stored with a placeholder to keep the values of the
     * same author at the same position. Such a value is a missing value, not a value out of the list.
     */
    private boolean isEmptyValueOfGroup(MetadataValue mdValue) {
        return CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE.equals(mdValue.getValue());
    }

    private boolean hasListOfValues(DCInput input) {
        return input.getPairs() != null && !input.isQualdropValue();
    }

    /**
     * Returns the form of the step and the nested forms of its group fields. For example the form
     * "publication-authors" declares the authors as a group, and the fields of one author, like its role, are in
     * the nested form "publication-authors-dc-contributor-author".
     */
    private List<DCInputSet> getForms(SubmissionStepConfig stepConfig) {
        try {
            if (inputsReader == null) {
                inputsReader = new DCInputsReader();
            }
            String formName = stepConfig.getId();
            List<DCInputSet> forms = new ArrayList<>();
            forms.add(inputsReader.getInputsByFormName(formName));
            forms.addAll(inputsReader.getInputsByGroup(formName));
            return forms;
        } catch (DCInputsReaderException e) {
            throw new IllegalStateException("Unable to read the forms of [" + stepConfig.getId() + "]", e);
        }
    }

    // GETTER & SETTERS ================================================================================================
    public void setInputsReader(DCInputsReader inputsReader) {
        this.inputsReader = inputsReader;
    }

    @Override
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
