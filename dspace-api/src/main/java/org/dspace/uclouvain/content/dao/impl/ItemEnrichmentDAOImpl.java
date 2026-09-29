/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.dao.impl;

import java.sql.SQLException;
import java.util.UUID;

import jakarta.persistence.TypedQuery;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.dao.ItemEnrichmentDAO;
import org.dspace.uclouvain.content.enrichment.ItemEnrichment;

/**
 * Hibernate implementation of {@link ItemEnrichmentDAO}.
 */
public class ItemEnrichmentDAOImpl extends AbstractHibernateDAO<ItemEnrichment> implements ItemEnrichmentDAO {

    @Override
    public ItemEnrichment findLatest(Context context, UUID itemUuid, String provider, String identifierValue)
        throws SQLException {
        TypedQuery<ItemEnrichment> query = getHibernateSession(context).createQuery("""
            FROM ItemEnrichment e
            WHERE e.itemUuid = :itemUuid AND e.provider = :provider AND e.identifierValue = :identifierValue
            ORDER BY e.attemptedAt DESC, e.id DESC
            """, ItemEnrichment.class);
        query.setParameter("itemUuid", itemUuid);
        query.setParameter("provider", provider);
        query.setParameter("identifierValue", identifierValue);
        query.setMaxResults(1);
        return query.getResultList().stream().findFirst().orElse(null);
    }
}
