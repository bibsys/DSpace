/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.content.integration.crosswalks.virtualfields;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} that returns the full handle link for an {@link Item}.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class VirtualFieldHandleLink implements VirtualField {

    protected static final Logger log = LogManager.getLogger(VirtualFieldHandleLink.class);

    @Autowired
    private HandleService handleService;

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {
        try {
            return new String[] { handleService.resolveToURL(context, item.getHandle()) };
        } catch (SQLException e) {
            log.warn("Unable to get handle URL for {}", item.getID(), e);
            return new String[] {};
        }
    }

}
