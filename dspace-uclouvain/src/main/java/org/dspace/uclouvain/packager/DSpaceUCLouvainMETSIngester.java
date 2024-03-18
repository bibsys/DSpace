package org.dspace.uclouvain.packager;

import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.MetadataValidationException;
import org.dspace.content.packager.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class DSpaceUCLouvainMETSIngester extends DSpaceMETSIngester {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AbstractMETSIngester.class);

    /**
     * Ingest/import a single DSpace Object, based on the associated METS
     * Manifest and the parameters passed to the METSIngester
     *
     * @param context  DSpace Context
     * @param parent   Parent DSpace Object
     * @param manifest the parsed METS Manifest
     * @param pkgFile  the full package file (which may include content files if a
     *                 zip)
     * @param params   Parameters passed to METSIngester
     * @param license  DSpace license agreement
     * @return completed result as a DSpace object
     * @throws IOException                 if IO error
     * @throws SQLException                if database error
     * @throws AuthorizeException          if authorization error
     * @throws CrosswalkException          if crosswalk error
     * @throws MetadataValidationException if metadata validation error
     * @throws WorkflowException           if workflow error
     * @throws PackageValidationException  if package validation error
     */
    protected DSpaceObject ingestObject(
                Context context, DSpaceObject parent, METSManifest manifest,
                File pkgFile, PackageParameters params, String license
    ) throws IOException, SQLException, AuthorizeException, CrosswalkException,
             PackageValidationException, WorkflowException {
        DSpaceObject dso = super.ingestObject(context, parent, manifest, pkgFile, params, license);
        this.updateObjectStatus(context, dso, manifest, params);
        return dso;
    }

    /** Enable `Item` withdrawn status if the METS manifest record status has 'inactive' value
     *
     * @param context     DSpace context
     * @param dso         DSpace object to manage
     * @param manifest    The parse METS manifest
     * @param params      Parameters passed to METSIngester
     *
     * @throws SQLException        if database error
     * @throws AuthorizeException  if authorization error
     */
    private void updateObjectStatus(
            Context context, DSpaceObject dso, METSManifest manifest, PackageParameters params
    ) throws SQLException, AuthorizeException {
        if (dso == null) {
            log.warn("Unable to update the object status :: object is null");
            return;
        }
        // Only ITEM object could have 'withdrawn' flag; but not for workflow items
        if (dso.getType() == Constants.ITEM && !params.workflowEnabled()) {
            String manifestRecordStatus = manifest.getRecordStatus();
            if (manifestRecordStatus != null && manifestRecordStatus.equalsIgnoreCase("inactive")) {
                Item item = (Item)dso;
                itemService.withdraw(context, item);
                itemService.update(context, item);
                log.debug("Enable withdrawn status for " + item);
            }
        }
    }
}
