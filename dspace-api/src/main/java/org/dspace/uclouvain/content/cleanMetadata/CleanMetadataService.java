/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.cleanMetadata;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service to clean fields that should not exist for a given type-bind type.
 * This is determined by looking at the configured type-bind field (default 'dc.type') of the item and by checking the
 * corresponding form configuration.
 * Typically, a form field that has a 'type-bind' linking to something else than the current DSpace item type should
 * be removed. This is done to avoid having useless values for the selected type.
 * Mainly used to clear unwanted data coming from Grobe plugin (aka grobid).
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public interface CleanMetadataService {

    default void cleanMetadata(Context context, Item item) throws SQLException, AuthorizeException {
        cleanMetadata(context, item, true);
    }

    /**
     * This method checks the item's metadata and cleans up the ones that should be hidden by type-bind.
     *
     * @param context the DSpace application context
     * @param item the {@link org.dspace.content.Item} to clean
     * @param autoUpdate is the item should be updated if any changes are made on it
     * @throws AuthorizeException if any authorization exception occurred
     * @throws SQLException if any database exception occurred
     */
    void cleanMetadata(Context context, Item item, boolean autoUpdate) throws AuthorizeException, SQLException;
}
