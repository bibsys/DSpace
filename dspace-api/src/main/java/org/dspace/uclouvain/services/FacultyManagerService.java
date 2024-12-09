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

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Interface for master thesis manager management
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 *
 */
public interface FacultyManagerService {

    /**
     * Get all eperson considering as faculty managers.
     *
     * @param context the application context
     * @return The list of managers
     */
    List<EPerson> getFacultyManagers(Context context) throws SQLException;

    /**
     * Get all eperson considering as faculty manager for a specific degree code.
     *
     * @param context the application context
     * @param degreeCode the root degree code to manage
     * @return The list of managers for this specific degreeCode
     * @throws SQLException for any database exception
     */
    List<EPerson> getFacultyManagers(Context context, String degreeCode) throws SQLException;

    /**
     * Is an eperson can be considered as a faculty manager.
     * To be faculty managers, the eperson must be member of "manager" group and have some degrees under "workingGroups"
     * data from their profile
     *
     * @param context the application context
     * @param person the eperson to analyze
     * @return True|False
     * @throws SQLException for any database exception
     */
    boolean isFacultyManager(Context context, EPerson person) throws SQLException;

    /**
     * Is an eperson manager for a specific degree?
     *
     * @param context the application context
     * @param person the eperson to analyze
     * @param degreeCode the root degree code to analyze
     * @return True|False
     */
    boolean isManagerFor(Context context, EPerson person, String degreeCode);

    /**
     * Get a list of degree code that an eperson is manager for.
     *
     * @param context the application context
     * @param person the eperson to analyze
     * @return the list of root degree code managed by this person.
     * @throws SQLException for any database exception
     * @throws AuthorizeException if current logged user isn't authorized.
     */
    List<String> getManagedDegree(Context context, EPerson person) throws SQLException, AuthorizeException;
}
