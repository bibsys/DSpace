/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.dao;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * UCLouvain specific database queries on {@link ResourcePolicy}.
 *
 * !!!This class should only be accessed from a single service and should never be exposed outside the API
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public interface UCLouvainResourcePolicyDAO {

    /**
     * Find the custom READ policies named "embargo" granted to a group whose start date is already reached.
     * Policies no longer attached to a DSpace object (their object was deleted) are ignored: there is nothing to
     * lift and nothing to notify.
     *
     * @param context the DSpace context
     * @param group   the group the policies are granted to
     * @param now     the reference date; policies starting strictly before it are returned
     * @return the matching policies, possibly empty
     * @throws SQLException if a database error occurs
     */
    List<ResourcePolicy> findExpiredEmbargoes(Context context, Group group, Date now) throws SQLException;
}
