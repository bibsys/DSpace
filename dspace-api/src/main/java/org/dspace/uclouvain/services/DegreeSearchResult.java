/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

/**
 * Record representing a degree found in the Solr index.
 *
 * @param degreeLabel       the degree label
 * @param degreeCode        the degree code
 * @param rootDegreeLabel   the root degree label
 * @param rootDegreeCode    the root degree code
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public record DegreeSearchResult(String degreeLabel, String degreeCode,
                                  String rootDegreeLabel, String rootDegreeCode) {

    /**
     * Compute how many fields of this result are filled (non-blank).
     * Used to keep the richest result when deduplicating.
     *
     * @return the number of non-blank fields (0 to 4)
     */
    public int completeness() {
        return (int) Stream.of(degreeLabel, degreeCode, rootDegreeLabel, rootDegreeCode)
            .filter(StringUtils::isNotBlank)
            .count();
    }
}
