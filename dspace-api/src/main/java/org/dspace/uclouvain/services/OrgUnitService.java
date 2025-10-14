/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.sql.SQLException;

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
}
