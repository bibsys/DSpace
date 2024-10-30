/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
@Component
@AuthorizationFeatureDocumentation(
    name = CanDownloadPDFAttestationFeature.NAME,
    description = "It can be used to verify if the attestation can be downloaded"
)
public class CanDownloadPDFAttestationFeature implements AuthorizationFeature {

    private static Logger logger = LogManager.getLogger(CanDownloadPDFAttestationFeature.class);
    public final static String NAME = "canDownloadPDFAttestation";

    @Autowired
    private ItemService itemService;
    @Autowired
    private Utils utils;
    @Autowired
    private AttestationAuthorizationService attestationAuthorizationService;

    /**
     * This method checks if a PDF attestation can be downloaded for a given item && user.
     * @param context the current DSpace context.
     * @param object  the object to check authorization for.
     * @return True if the user is authorized to download the attestation of the item.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) {
        try {
            DSpaceObject dsObject = (DSpaceObject)utils.getDSpaceAPIObjectFromRest(context, object);
            Item dsItem = itemService.find(context, dsObject.getID());
            if (dsItem == null) {
                return false;
            }
            return attestationAuthorizationService.isItemValidForAttestation(dsItem, context)
                && attestationAuthorizationService.isUserAuthorized(dsItem, context);
        } catch (SQLException e) {
            logger.warn("Could not check for PDF attestation download authorization", e);
            return false;
        }

    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] {
            ItemRest.CATEGORY + "." + ItemRest.NAME
        };
    }
}
