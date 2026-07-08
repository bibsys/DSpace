/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.core.utils.PublicationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AuthorizationFeatureDocumentation(name = HasAuthorEmailFeature.NAME,
    description = "It can be used to verify if a publications has at least one author email")
public class HasAuthorEmailFeature implements AuthorizationFeature {
    public final static String NAME = "hasAuthorEmail";

    @Autowired
    private ItemService itemService;
    @Autowired
    private BitstreamService bitstreamService;

    @Override
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        Item item = null;
        if (object instanceof ItemRest itemRest) {
            item = itemService.find(context, UUID.fromString(itemRest.getId()));
        } else if (object instanceof BitstreamRest bitstreamRest) {
            Bitstream bitstream = bitstreamService.find(context, UUID.fromString(bitstreamRest.getId()));
            if (bitstream != null) {
                DSpaceObject parentObject = bitstreamService.getParentObject(context, bitstream);
                if (parentObject instanceof Item) {
                    item = (Item) parentObject;
                }
            }
        }
        return item != null && PublicationUtils.existsPersistentRecipient(item);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }
}
