/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.OrgUnit;

public interface OrgUnitService {

    /**
     * Find an `OrgUnit` based on institution and/or entity name/acronym
     * @param context The DSpace application context
     * @param institutionAcronym the institution acronym
     * @param institutionName the institution name
     * @param entityAcronym the entity acronym
     * @param entityName the entity name
     * @return the corresponding entity
     * @throws SQLException if any database error occurred
     * @throws IllegalArgumentException if all search criteria are empty
     */
    OrgUnit findByName(
        Context context,
        String institutionAcronym,
        String institutionName,
        String entityAcronym,
        String entityName
    ) throws SQLException;

    /**
     * Find matching OrgUnits for a given list of affiliation names.
     * Each affiliations has to be splitted into levels based on the '/' element.
     * We only work with level 3 and 2 so affiliations of only 1 level are ignored.
     * Ex. of accepted affiliation: ['LS/SGSI/SISG', 'SSS/FASB'].
     * Ex. of ignored affiliation: ['StLuc', 'SSH'].
     * @param context The current Dspace context.
     * @param affiliations A list of affiliations to search on.
     * @return A list of OrgUnit representing the matching affiliations for the given list.
     * Can return null if no matching affiliation are found.
     */
    List<OrgUnit> findByName(Context context, List<String> affiliations);

    /**
     * Find first matching OrgUnit from a list of affiliations.
     * @param context The current DSpace context.
     * @param affiliations The list of affiliations to find matching orgUnit of.
     */
    OrgUnit findFirstByName(Context context, List<String> affiliations);
}
