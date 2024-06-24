package org.dspace.app.rest.security;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.UUID;

import org.dspace.services.model.Request;
import org.dspace.uclouvain.core.utils.AuthorizationUtils;
import org.dspace.uclouvain.core.utils.ItemUtils;
import org.dspace.uclouvain.rest.BitstreamDirectDownloadRestController;
import org.dspace.web.ContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.RequestService;

/** 
 * EvaluatorPlugin which deals with the permissions for the bitstream download URL generation.
 * 
 * @Author: Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */ 
@Component
public class BitstreamDirectDownloadURLPermissionEvaluatorPlugin extends RestObjectPermissionEvaluatorPlugin {

    private final static String DOWNLOAD_URL_PERMISSION = "DOWNLOAD_URL";

    @Autowired
    private RequestService requestService;

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private ItemUtils itemUtils;

    @Autowired
    private AuthorizationUtils authUtils; 

    @Autowired
    private AuthorizeService authorizeService;

    private Logger logger = LogManager.getLogger(BitstreamDirectDownloadRestController.class);

    /**
     * Handles the permission evaluation for the download URL generation.
     * Example of call to this method with the '@PreAuthorize' decorator: `@PreAuthorize("hasPermission(#bitstreamId, 'BITSTREAM', 'DOWNLOAD_URL')")`.
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (permission.toString().equalsIgnoreCase(DOWNLOAD_URL_PERMISSION) && targetId != null && targetType.equals("BITSTREAM")) {
            Request request = this.requestService.getCurrentRequest();
            Context context = ContextUtil.obtainContext(request.getHttpServletRequest());

            EPerson currentUser = context.getCurrentUser();
            
            // Check if user is logged in
            if (currentUser != null) {
                try {
                    Bitstream targetBitstream = this.bitstreamService.find(context, UUID.fromString(targetId.toString()));
                    Item item = this.itemUtils.getItemFromBitstream(context, targetBitstream);
                    if (item != null) {
                        return this.isAuthorized(context, item, currentUser);
                    }
                } catch (Exception e) {
                    logger.error("Error in custom BitstreamDownloadURLPermissionEvaluatorPlugin: " + e.getMessage());
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasDSpacePermission(Authentication authentication, Serializable targetId, String targetType, DSpaceRestPermission restPermission) {
        return false;
    }

    // Checks if with the current context (item && user) we can generate a download URL.
    private boolean isAuthorized(Context context, Item item, EPerson currentUser) {
        try {
            return this.itemUtils.isWorkflow(context, item) && this.isUserAuthorized(context, item, currentUser); 
        } catch (SQLException e) {
            logger.warn("An error occurred while checking the authorization for the download URL generation with item UUID: " + item.getID() + " && user: " + currentUser.getEmail() + "; " + e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the user has authorization to access the download URL.
     * The user has authorization if:
     * - User is an admin -OR-
     * - User is a manager for the collection
     * @param context: The current Dspace Context
     * @param item: The item to check for authorization.
     * @param currentUser: The current logged user.
     * @return True if the user meets one of the requirements listed above, false otherwise.
     * @throws SQLException
     */
    private boolean isUserAuthorized(Context context, Item item, EPerson currentUser) throws SQLException {
        return this.authorizeService.isAdmin(context, currentUser) ||
            this.authUtils.isManagerOfItem(context, item, currentUser);
    }
}
