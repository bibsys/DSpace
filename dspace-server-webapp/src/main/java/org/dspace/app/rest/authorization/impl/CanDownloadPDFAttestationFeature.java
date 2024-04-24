package org.dspace.app.rest.authorization.impl;

import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.uclouvain.pdfAttestationGenerator.AttestationAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Rule to indicate if a PDF attestation can be downloaded for a given item && user.
 */
@Component
@AuthorizationFeatureDocumentation(
    name = CanDownloadPDFAttestationFeature.NAME,
    description = "It can be used to verify if the attestation can be downloaded"
)
public class CanDownloadPDFAttestationFeature implements AuthorizationFeature {
    public final static String NAME = "canDownloadPDFAttestation";

    @Autowired
    private ItemService itemService;

    @Autowired
    private Utils utils;

    @Autowired
    private AttestationAuthorizationService attestationAuthorizationService;

    private static Logger logger = LogManager.getLogger(CanDownloadPDFAttestationFeature.class);

    /**
     * This method checks if a PDF attestation can be downloaded for a given item && user.
     * @param context: The current DSpace context.
     * @param object: The object to check authorization for.
     * @return True if the user is authorized to download the attestation of the item.
     * @throws SQLException If something goes wrong.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) {
        try {
            DSpaceObject dsObject = (DSpaceObject)utils.getDSpaceAPIObjectFromRest(context, object);
            Item dsItem = itemService.find(context, dsObject.getID());
            if (dsItem == null) return false;
            return attestationAuthorizationService.isItemValidForAttestation(dsItem, context) 
            && attestationAuthorizationService.isUserAuthorized(dsItem, context);
        } catch (SQLException e) {
            logger.warn("Could not check for PDF attestation download authorization", e);
            return false;
        }

    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {ItemRest.CATEGORY + "." + ItemRest.NAME};
    }
}
