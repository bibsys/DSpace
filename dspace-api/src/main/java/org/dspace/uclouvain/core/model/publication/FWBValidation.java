/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core.model.publication;

import org.apache.commons.lang3.tuple.Pair;
import org.dspace.core.Context;

public interface FWBValidation {
    int DECREE_YEAR = 2018;

    Pair<Boolean, String> VALIDATION_SUCCESS = Pair.of(true, null);
    Pair<Boolean, String> VALIDATION_FAILURE_NO_FILE = Pair.of(false, "error.validation.fwb.nofile");
    Pair<Boolean, String> VALIDATION_FAILURE_NO_DATE = Pair.of(false, "error.validation.fwb.no-date");
    Pair<Boolean, String> VALIDATION_FAILURE_ACCESS_TYPE = Pair.of(false, "error.validation.fwb.accesstype");
    Pair<Boolean, String> VALIDATION_FAILURE_EMBARGO_DATE = Pair.of(false, "error.validation.fwb.wrongembargodate");

    default Pair<Boolean, String> isFWBCompliant(Context context) {
        return VALIDATION_SUCCESS;
    }

    default boolean isFWBExportable(Context context) {
        return isFWBCompliant(context).getLeft();
    }
}
