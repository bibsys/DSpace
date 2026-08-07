/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

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
}
