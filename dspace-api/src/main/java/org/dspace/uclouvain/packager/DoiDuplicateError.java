/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.packager;

import java.util.List;

import org.dspace.validation.model.ValidationError;

/**
 * The validation error of an ingested item whose DOI is already the DOI of an archived item. It tells which
 * archived item, so that the producer of the package can look at it.
 */
public class DoiDuplicateError extends ValidationError {

    public static final String ERROR_VALIDATION_DOI_DUPLICATE = "error.validation.doi.duplicate";

    private final String duplicateHandle;

    /**
     * @param doiPath         the path of the DOI in the ingested item, for example "dc.identifier.doi/0"
     * @param duplicateHandle the handle of the archived item having the same DOI
     */
    public DoiDuplicateError(String doiPath, String duplicateHandle) {
        super(ERROR_VALIDATION_DOI_DUPLICATE);
        setPaths(List.of(doiPath));
        this.duplicateHandle = duplicateHandle;
    }

    public String getDuplicateHandle() {
        return duplicateHandle;
    }
}
