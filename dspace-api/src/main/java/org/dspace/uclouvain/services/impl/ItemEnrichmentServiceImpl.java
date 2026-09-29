/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.services.impl;

import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.uclouvain.content.dao.ItemEnrichmentDAO;
import org.dspace.uclouvain.content.enrichment.ItemEnrichment;
import org.dspace.uclouvain.services.ItemEnrichmentService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default implementation of {@link ItemEnrichmentService}.
 */
public class ItemEnrichmentServiceImpl implements ItemEnrichmentService {

    private static final int REASON_MAX_LENGTH = 1024;

    @Autowired
    private ItemEnrichmentDAO itemEnrichmentDAO;

    @Override
    public ItemEnrichment findLatest(
        Context context,
        Item item,
        String provider,
        String identifierValue
    ) throws SQLException {
        return itemEnrichmentDAO.findLatest(context, item.getID(), provider, identifierValue);
    }

    @Override
    public ItemEnrichment record(
        Context context,
        Item item,
        String provider,
        String identifierField,
        String identifierValue,
        ItemEnrichment.Status status,
        String reason
    ) throws SQLException {
        ItemEnrichment attempt = new ItemEnrichment();
        attempt.setItemUuid(item.getID());
        attempt.setProvider(provider);
        attempt.setIdentifierField(identifierField);
        attempt.setIdentifierValue(identifierValue);
        attempt.setStatus(status);
        attempt.setReason(StringUtils.abbreviate(reason, REASON_MAX_LENGTH));
        attempt.setAttemptedAt(new Date());
        return itemEnrichmentDAO.create(context, attempt);
    }
}
