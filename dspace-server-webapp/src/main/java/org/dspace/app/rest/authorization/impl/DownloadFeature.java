/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;
import java.sql.SQLException;

import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.authorization.AuthorizeServiceRestUtil;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.security.BitstreamCrisSecurityService;
import org.dspace.app.rest.security.DSpaceRestPermission;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.uclouvain.core.utils.AuthorizationUtils;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The download bitstream feature. It can be used to verify if a bitstream can be downloaded.
 * Authorization is granted if the current user has READ permissions on the given bitstream.
 * <p>
 * Additionally, users with the librarian role are granted download access to all archived
 * bitstreams when the feature is enabled via {@code uclouvain.feature.librarian.bitstream.read.enabled}.
 * </p>
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
@Component
@AuthorizationFeatureDocumentation(name = DownloadFeature.NAME,
        description = "It can be used to verify if the user can download a bitstream")
public class DownloadFeature implements AuthorizationFeature {

    public final static String NAME = "canDownload";

    private static final Logger log = LoggerFactory.getLogger(DownloadFeature.class);

    @Autowired
    private AuthorizeServiceRestUtil authorizeServiceRestUtil;

    @Autowired
    private BitstreamCrisSecurityService bitstreamCrisSecurityService;

    @Autowired
    private Utils utils;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AuthorizationUtils authorizationUtils;

    @Autowired
    private ItemUtils itemUtils;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {

        if (object instanceof BitstreamRest) {
            if (authorizeServiceRestUtil.authorizeActionBoolean(context, object, DSpaceRestPermission.READ)) {
                return true;
            }
        }
        try {
            DSpaceObject dSpaceObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);
            if (dSpaceObject == null) {
                return false;
            }

            if (dSpaceObject instanceof Bitstream && bitstreamCrisSecurityService
                    .isBitstreamAccessAllowedByCrisSecurity(context, context.getCurrentUser(),
                            (Bitstream) dSpaceObject)) {
                return true;
            }
        } catch (Exception e) {
            log.warn(
                    "We got an exception during the cris security evaluation, safe fallback " +
                    "ignoring extra grant given by cris",
                    e);
        }

        // Check if librarian has access to download archived bitstreams
        if (isLibrarianDownloadEnabled() && object instanceof BitstreamRest) {
            try {
                EPerson currentUser = context.getCurrentUser();
                if (currentUser != null && authorizationUtils.isLibrarian(context, currentUser)) {
                    DSpaceObject dSpaceObject = (DSpaceObject) utils.getDSpaceAPIObjectFromRest(context, object);
                    if (dSpaceObject instanceof Bitstream bitstream) {
                        Item item = itemUtils.getItemFromBitstream(context, bitstream);
                        if (item != null && !ItemUtils.isWorkspace(context, item)) {
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("We got an exception during the librarian download evaluation, safe fallback", e);
            }
        }

        return false;
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[]{
            BitstreamRest.CATEGORY + "." + BitstreamRest.NAME,
        };
    }

    /**
     * Check if the librarian bitstream download feature is enabled.
     *
     * @return true if enabled (default: false), false otherwise
     */
    protected boolean isLibrarianDownloadEnabled() {
        return configurationService.getBooleanProperty(
            "uclouvain.feature.librarian.bitstream.read.enabled", false);
    }
}
