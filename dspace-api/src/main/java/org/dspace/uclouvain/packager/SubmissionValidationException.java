/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.packager;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.content.packager.PackageValidationException;
import org.dspace.validation.model.ValidationError;

/**
 * The ingested item was refused by the submission validators. It has been deleted within the current
 * transaction: the caller must commit that transaction, otherwise the deletion is lost and the files of the
 * item stay in the assetstore.
 *
 * @author Renaud Michotte <renaud.michotte@uclouvain.be>
 */
public class SubmissionValidationException extends PackageValidationException {

    private final transient List<ValidationError> errors;

    /**
     * @param errors the errors reported by the submission validators.
     */
    public SubmissionValidationException(List<ValidationError> errors) {
        super("The item doesn't pass the submission validation: " + describe(errors));
        this.errors = List.copyOf(errors);
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    private static String describe(List<ValidationError> errors) {
        return errors.stream()
            .map(SubmissionValidationException::describe)
            .collect(Collectors.joining("; "));
    }

    private static String describe(ValidationError error) {
        String description = error.getMessage() + " " + error.getPaths();
        if (error instanceof DoiDuplicateError doiDuplicate) {
            description += " (same DOI as " + doiDuplicate.getDuplicateHandle() + ")";
        }
        return description;
    }
}
