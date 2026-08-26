/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.validation;

import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;
import static org.dspace.validation.util.ValidationUtils.addError;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.MasterThesis;
import org.dspace.uclouvain.content.MasterThesisAuthor;
import org.dspace.uclouvain.content.MasterThesisDegree;
import org.dspace.validation.SubmissionStepValidator;
import org.dspace.validation.model.ValidationError;

/**
 * Submission Step validator to confirm that every author of a master thesis has a degree code.
 * If it is not the case, the user should not be able to submit and an error will be returned.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class MasterThesisAuthorDegreeValidator implements SubmissionStepValidator {
    private String name;
    private List<String> targetedSubmissionSteps = new ArrayList<>();

    private static final String VALIDATION_FAILURE_MISSING_DEGREE = "error.validation.authors.missing-degree";

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        if (!targetedSubmissionSteps.contains(config.getId())) {
            return Collections.emptyList();
        }
        List<ValidationError> errors = new ArrayList<>();
        Item item = obj.getItem();
        MasterThesis masterThesis = new MasterThesis(item);

        // Check if every author has a degree code associated to him.
        degreeCheck(masterThesis, errors, config);
        return errors;
    }

    /**
     * Verify that the master thesis item has a degree for each of its author.
     * 
     * @param masterThesis The master thesis item.
     * @param errors The validation errors to return the the UI.
     * @param config The current submission step config.
     */
    private void degreeCheck(
        MasterThesis masterThesis, List<ValidationError> errors, SubmissionStepConfig config
    ) {
        List<MasterThesisAuthor> authors = masterThesis.getAuthors().stream().filter(Objects::nonNull).toList();
        List<MasterThesisDegree> degrees = masterThesis.getDegrees().stream().filter(Objects::nonNull).toList();

        boolean hasMissingDegree = IntStream.range(0, authors.size())
            .anyMatch(i -> i >= degrees.size() || PLACEHOLDER_PARENT_METADATA_VALUE.equals(degrees.get(i).degreeCode));

        if (hasMissingDegree) {
            addError(
                errors,
                VALIDATION_FAILURE_MISSING_DEGREE,
                "/" + OPERATION_PATH_SECTIONS + "/" + config.getId()
            );
        }
    }

    // GETTERS & SETTERS

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTargetedSubmissionSteps() {
        return targetedSubmissionSteps;
    }

    public void setTargetedSubmissionSteps(List<String> targetedSubmissionSteps) {
        this.targetedSubmissionSteps = targetedSubmissionSteps;
    }
}
