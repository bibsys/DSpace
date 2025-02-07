/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.plugins;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.dspace.access.status.AccessStatusHelper;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.factories.UCLouvainServiceFactory;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

/**
 * UCLouvain plugin implementation of the access status helper.
 *
 * The `getAccessStatusFromItem` method provides a simple logic to
 * calculate the access status of an item based on the policies of
 * the primary or the first bitstream in the original bundle. If no
 * bitstream could be found or no policies are defined into bitstream,
 * the access could also be found into a specific metadata field
 * (default: "dcterms.accessRights").
 *
 * The `getEmbargoInformationFromItem` method provides a simple logic to
 * retrieve embargo information of bitstreams from an item based on the policies of
 * the primary or the first bitstream in the original bundle.
 *
 * @author Renaud Michotte (renaud.michotte@uclouvain.be)
 */
public class UCLouvainAccessStatusHelper implements AccessStatusHelper {
    public static final String ADMINISTRATOR = "administrator";
    public static final String RESTRICTED = "restricted";
    public static final String EMBARGO = "embargo";
    public static final String OPEN_ACCESS = "openaccess";
    public static final String UNKNOWN = "unknown";

    private final MetadataFieldName accessMetadataFieldName;

    protected ContentServiceFactory contentFactory = ContentServiceFactory.getInstance();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    protected UCLouvainResourcePolicyService uclouvainResourcePolicyService =
            UCLouvainServiceFactory.getInstance().getResourcePolicyService();


    public UCLouvainAccessStatusHelper() {
        this.accessMetadataFieldName = new MetadataFieldName(configurationService
                .getProperty("uclouvain.global.metadata.accesstype.field", "dcterms.accessRights"));
    }

    /**
     * Look at the item's policies to determine an access status value.
     * It is also considering a date threshold for embargoes and restrictions.
     * If the item is null, simply return the "unknown" value.
     *
     * @param context     the DSpace context
     * @param item        the item to check for embargoes
     * @param threshold   the embargo threshold date
     * @return an access status value
     */
    @Override
    public String getAccessStatusFromItem(Context context, Item item, Date threshold) throws SQLException {
        if (item == null) {
            return UNKNOWN;
        }
        Bitstream masterBitstream = this.getMasterBitstreamForItem(context, item);
        return (masterBitstream != null)
            ? calculateAccessStatusForDso(context, masterBitstream)
            : UNKNOWN;
    }

    /**
     * Look at the policies of the primary (or first) bitstream of the item to retrieve its embargo.
     * Return null if the item is null or master bitstream isn't embargoed.
     *
     * @param context     the DSpace context
     * @param item        the item to embargo
     * @return an access status value
     */
    @Override
    public String getEmbargoFromItem(Context context, Item item, Date threshold) throws SQLException {
        // If Item status is not "embargo" then return a null embargo date.
        String accessStatus = getAccessStatusFromItem(context, item, threshold);
        if (item == null || !accessStatus.equals(EMBARGO)) {
            return null;
        }
        // Get the master bitstream about this item... it should return an embargoed bitstream.
        Bitstream masterBitstream = getMasterBitstreamForItem(context, item);
        if (masterBitstream == null) {
            return null;
        }
        Date embargoDate = this.retrieveEmbargo(context, masterBitstream);
        return (embargoDate != null)
            ? embargoDate.toString()
            : null;
    }

    /**
     * Get the master bitstream for an Item. Master bitstream is either the
     * defined item primary bitstream, either the first bitstream of the default bundle.
     *
     * @param context the application context
     * @param item the item to analyze
     * @return the master item bitstream if exists, otherwise return null.
     */
    private Bitstream getMasterBitstreamForItem(Context context, @NotNull Item item) {
        List<Bundle> bundles = item.getBundles(Constants.DEFAULT_BUNDLE_NAME);

        // 1) Try to find a bitstream flagged as `primary`.
        //    If we find one, then use it!
        Bitstream primaryBitstream = bundles
                .stream()
                .map(Bundle::getPrimaryBitstream)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (primaryBitstream != null) {
            return primaryBitstream;
        }

        // 2) No primary bitstream has been found.
        //    So we need to find the most permissive bitstream
        return bundles
                .stream()
                .flatMap(bundle -> bundle.getBitstreams().stream())
                .max(Comparator.comparingInt(b -> getBitstreamPriority(context, b)))
                .orElse(null);
    }

    /**
     * Find the bitstream priority based on bitstream related resource policy.
     *
     * @param context the application context
     * @param bitstream the bitstream to analyze
     * @return the bitstream priority; larger number = higher priority.
     */
    private int getBitstreamPriority(Context context, Bitstream bitstream) {
        try {
            String accessStatus = calculateAccessStatusForDso(context, bitstream);
            return uclouvainResourcePolicyService.getPolicyWeight(accessStatus);
        } catch (SQLException sqle) {
            return Integer.MIN_VALUE;
        }
    }


    /**
     * Look at the DSpace object's policies to determine an access status value.
     * If the object is null, returns the "metadata.only" value.
     * If any policy attached to the object is valid for the anonymous group,
     * returns the "open.access" value.
     * Otherwise, if the policy start date is before the embargo threshold date,
     * returns the "embargo" value.
     * Every other case returns the "restricted" value.
     *
     * @param context     the DSpace context
     * @param dso         the DSpace object
     * @return an access status value
     */
    public String calculateAccessStatusForDso(Context context, DSpaceObject dso) throws SQLException {
        if (dso != null) {
            List<ResourcePolicy> policies = uclouvainResourcePolicyService.find(context, dso);
            ResourcePolicy masterPolicy = uclouvainResourcePolicyService.getMasterPolicy(policies);
            if (masterPolicy != null && StringUtils.isNotEmpty(masterPolicy.getRpName())) {
                return getControlledAccessValue(masterPolicy.getRpName());
            }
        }
        String accessValue = getAccessFromMetadata(dso);

        // Special case for `Bitstream`:
        //   If nor policies are found, nor specific metadata,
        //   then the access status isn't UNKNOWN but OPEN_ACCESS
        if (accessValue.equals(UNKNOWN) && dso instanceof Bitstream) {
            return OPEN_ACCESS;
        }
        return getControlledAccessValue(accessValue);
    }

    /**
     * Get the first access rights value into DSpaceObject metadata list.
     *
     * @param dso The DspaceObject to analyze
     * @return the corresponding access right metadata value, or null if not find.
     */
    private String getAccessFromMetadata(DSpaceObject dso) {
        try {
            DSpaceObjectService<DSpaceObject> service = contentFactory.getDSpaceObjectService(dso);
            String metadataValue = service.getMetadataFirstValue(dso, accessMetadataFieldName, "*");
            return (StringUtils.isNotEmpty(metadataValue)) ? metadataValue : UNKNOWN;
        } catch (UnsupportedOperationException uoe) {
            return UNKNOWN;
        }
    }

    /**
     * Retrieve the embargo date about a bitstream checking the related resource policies.
     *
     * @param context    the DSpace context
     * @param bitstream  the DSpace bitstream to analyze
     * @return the corresponding embargo start date if bitstream is embargoed.
     * @throws SQLException for any database exception
     */
    private Date retrieveEmbargo(Context context, Bitstream bitstream) throws SQLException {
        List<ResourcePolicy> policies = uclouvainResourcePolicyService.find(context, bitstream);
        ResourcePolicy masterPolicy = uclouvainResourcePolicyService.getMasterPolicy(policies);
        return (masterPolicy != null && masterPolicy.getRpName().equals(EMBARGO))
            ? masterPolicy.getStartDate()
            : null;
    }

    /**
     * Convert a access value string to a controlled vocabulary entry
     *
     * @param initialValue the access value to convert.
     * @return the converted access value.
     */
    public static String getControlledAccessValue(String initialValue) {
        if (initialValue == null || StringUtils.isEmpty(initialValue)) {
            // !!! It should never happen if access conditions are set using the submission form !!!
            //     The submission form used the value from the select input field as rpName for a resource policy
            //     Every select input field entry has a value. So if the value is empty, this is because an
            //     admin uses the resource policy editor
            return OPEN_ACCESS;
        }
        switch (initialValue.trim().toLowerCase()) {
            case "openaccess": return OPEN_ACCESS;
            case "administrator": return ADMINISTRATOR;
            case "embargo": return EMBARGO;
            default: return RESTRICTED;
        }
    }
}
