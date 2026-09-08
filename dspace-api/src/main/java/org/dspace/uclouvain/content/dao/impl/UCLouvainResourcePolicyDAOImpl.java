/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.dao.impl;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.uclouvain.content.dao.UCLouvainResourcePolicyDAO;
import org.dspace.uclouvain.plugins.UCLouvainAccessStatusHelper;

/**
 * Hibernate implementation of {@link UCLouvainResourcePolicyDAO}.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class UCLouvainResourcePolicyDAOImpl extends AbstractHibernateDAO<ResourcePolicy>
    implements UCLouvainResourcePolicyDAO {

    @Override
    public List<ResourcePolicy> findExpiredEmbargoes(Context context, Group group, Date now) throws SQLException {
        String queryString = """
            SELECT p FROM ResourcePolicy p
            WHERE p.actionId = :action
              AND p.rptype = :type
              AND p.rpname = :name
              AND p.epersonGroup = :group
              AND p.startDate < :now
              AND p.dSpaceObject IS NOT NULL
            ORDER BY p.startDate ASC, p.id ASC
            """;
        return getHibernateSession(context)
            .createQuery(queryString, ResourcePolicy.class)
            .setParameter("action", Constants.READ)
            .setParameter("type", ResourcePolicy.TYPE_CUSTOM)
            .setParameter("name", UCLouvainAccessStatusHelper.EMBARGO)
            .setParameter("group", group)
            .setParameter("now", now)
            .getResultList();
    }
}
