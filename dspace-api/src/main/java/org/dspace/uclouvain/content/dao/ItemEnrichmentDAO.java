/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.dao;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.core.Context;
import org.dspace.core.GenericDAO;
import org.dspace.uclouvain.content.enrichment.ItemEnrichment;

/**
 * DAO of the {@link ItemEnrichment} attempts.
 */
public interface ItemEnrichmentDAO extends GenericDAO<ItemEnrichment> {

    /**
     * The most recent attempt for an item, a provider and an identifier value.
     *
     * @return the latest attempt, or null when none was made.
     */
    ItemEnrichment findLatest(
        Context context,
        UUID itemUuid,
        String provider,
        String identifierValue
    )throws SQLException;
}
