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
import java.util.Map;
import java.util.UUID;

import org.dspace.core.Context;
import org.dspace.uclouvain.core.model.OrgUnit;

public interface OrgUnitService {

    /**
     * Find an `OrgUnit` by its UUID.
     * @param context The current DSpace application context
     * @param uuid UUID of the OrgUnit to find
     * @return the corresponding OrgUnit, null if not found
     */
    OrgUnit findByIdentifier(Context context, String uuid);

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
     * Find all matching OrgUnits for a given name (dc.title).
     * @param context The current Dspace application context.
     * @param name The name to search for.
     * @return A list of matching OrgUnits. Empty list if no matching OrgUnit found.
     */
    List<OrgUnit> findByName(Context context, String name);

    /**
     * Find matching OrgUnits for a given list of affiliation names.
     * Each affiliation has to be split into levels based on the '/' element.
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

    /**
     * Find all stored and indexed {@link OrgUnit}
     * @param context The current DSpace context.
     * @return the list of all known OrgUnit
     */
    List<OrgUnit> findAll(Context context);

    /**
     * Get the dictionary of publication count related to {@link OrgUnit}
     * @param context The current DSpace context.
     * @return a map where each entry represent the {@link OrgUnit} as key, and publication count as value
     */
    Map<UUID, Long> getPublicationCount(Context context);
}
