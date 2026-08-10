/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.dspace.uclouvain.core.utils.AuthorizationUtils;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Permission evaluator plugin that grants READ permission on bitstreams
 * to users with the librarian role.
 * <p>
 * This plugin allows librarians to download any bitstream present in
 * the archive, regardless of the collection-level resource policies.
 * The feature only applies to bitstreams belonging to archived items
 * (not items in workflow or workspace).
 * </p>
 * <p>
 * Feature can be toggled via the configuration property:
 * <pre>
 * uclouvain.feature.librarian.bitstream.read.enabled=true
 * </pre>
 * </p>
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
@Component
public class BitstreamLibrarianReadPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ItemUtils itemUtils;

    @Autowired
    private AuthorizationUtils authorizationUtils;

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId,
                                       String targetType, DSpaceRestPermission restPermission) {
        // Only handle READ permission on BITSTREAM objects
        if (!"BITSTREAM".equals(targetType) || !DSpaceRestPermission.READ.equals(restPermission)) {
            return false;
        }

        // Check if feature is enabled via configuration
        if (!isFeatureEnabled()) {
            return false;
        }

        Request request = requestService.getCurrentRequest();
        Context context = ContextUtil.obtainContext(request.getHttpServletRequest());
        EPerson currentUser = context.getCurrentUser();

        // User must be logged in
        if (currentUser == null) {
            return false;
        }

        // Check if user is a librarian
        if (!authorizationUtils.isLibrarian(context, currentUser)) {
            return false;
        }

        // Verify the bitstream belongs to an archived item
        try {
            Bitstream bitstream = bitstreamService.find(context, UUID.fromString(targetId.toString()));
            if (bitstream == null) {
                return false;
            }

            Item item = itemUtils.getItemFromBitstream(context, bitstream);
            if (item == null) {
                return false;
            }

            // Only allow access to archived or workflow items (not in workspace)
            return !ItemUtils.isWorkspace(context, item);
        } catch (SQLException e) {
            log.error("Error checking librarian bitstream access for targetId: " + targetId, e);
            return false;
        }
    }

    /**
     * Check if the librarian bitstream read feature is enabled.
     *
     * @return true if enabled (default: false), false otherwise
     */
    protected boolean isFeatureEnabled() {
        return configurationService.getBooleanProperty(
            "uclouvain.feature.librarian.bitstream.read.enabled", false);
    }
}
