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
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.publication.Publication;
import org.dspace.uclouvain.core.model.publication.PublicationFactory;
import org.dspace.validation.SubmissionStepValidator;
import org.dspace.validation.model.ValidationError;

/**
 * Validator for FWB specifications, see `https://gallilex.cfwb.be/sites/default/files/imports/45142_000.pdf`.
 * All articles published after 2020 must have a global access type of either 'openaccess' or 'embargo'.
 * If the access type is not right, we must send an error to the form so that the user can correct the bitstream(s).
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class FWBAccessTypeValidator implements SubmissionStepValidator {
    private String name;
    private static final Logger logger = LogManager.getLogger(FWBAccessTypeValidator.class);

    /**
     * Validate the given submission step ("upload") by using the FWBValidationService.
     * 
     * @param context The current DSpace context.
     * @param obj The 'in progress submission' object from which we can extract the item.
     * @param config The submission step we are validating. (In this case "upload").
     */
    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        List<ValidationError> errors = new ArrayList<>();
        try {
            Publication publication = PublicationFactory.build(obj.getItem());
            Pair<Boolean, String> compliant = publication.isFWBCompliant(context);
            if (!compliant.getLeft()) {
                addError(
                    errors,
                    compliant.getRight(),
                    "/" + OPERATION_PATH_SECTIONS + "/" + config.getId()
                );
            }
        } catch (Exception e) {
            logger.warn("An error occurred while validating FWB specs for item with uuid: {}", obj.getID(), e);
        }
        return errors;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
