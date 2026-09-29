/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.enrichment.ItemEnrichment;

/**
 * Keeps track of the attempts to enrich items from the external sources, so that a source is not queried again
 * for an identifier it already answered.
 */
public interface ItemEnrichmentService {

    /**
     * The most recent attempt for an item, a provider and an identifier value.
     *
     * @param context         The DSpace context.
     * @param item            The item.
     * @param provider        The provider source identifier (crossref, pubmed, arxiv...).
     * @param identifierValue The identifier value that was queried.
     * @return the latest attempt, or null when none was made.
     */
    ItemEnrichment findLatest(
        Context context,
        Item item,
        String provider,
        String identifierValue
    ) throws SQLException;

    /**
     * Record an attempt.
     *
     * @param context         The DSpace context.
     * @param item            The item.
     * @param provider        The provider source identifier.
     * @param identifierField The metadata field the identifier comes from (dc.identifier.doi...).
     * @param identifierValue The identifier value that was queried.
     * @param status          The outcome.
     * @param reason          The failure message for an error, null otherwise; truncated to the column size.
     * @return the recorded attempt.
     */
    ItemEnrichment record(
        Context context,
        Item item,
        String provider,
        String identifierField,
        String identifierValue,
        ItemEnrichment.Status status,
        String reason
    ) throws SQLException;
}
