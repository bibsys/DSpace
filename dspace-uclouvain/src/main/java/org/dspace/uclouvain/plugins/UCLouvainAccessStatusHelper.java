package org.dspace.uclouvain.plugins;

import org.apache.commons.lang3.StringUtils;
import org.dspace.access.status.AccessStatusHelper;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.uclouvain.factories.UCLouvainResourcePolicyServiceFactory;
import org.dspace.uclouvain.services.UCLouvainResourcePolicyService;

import javax.validation.constraints.NotNull;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
    protected UCLouvainResourcePolicyService uclouvainResourcePolicyService = UCLouvainResourcePolicyServiceFactory.getInstance().getResourcePolicyService();


    public UCLouvainAccessStatusHelper() {
        String fieldName = configurationService.getProperty("global.access.metadata.field", "dcterms.accessRights");
        this.accessMetadataFieldName = new MetadataFieldName(fieldName);
    }

    /**
     * Look at the item's policies to determine an access status value.
     * It is also considering a date threshold for embargoes and restrictions.
     * If the item is null, simply returns the "unknown" value.
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
        Bitstream masterBitstream = this.getMasterBitstreamForItem(item);
        return (masterBitstream != null)
                ? calculateAccessStatusForDso(context, masterBitstream)
                : getAccessFromMetadata(item);
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
        Bitstream masterBitstream = getMasterBitstreamForItem(item);
        if (masterBitstream == null) {
            return null;
        }
        Date embargoDate = this.retrieveEmbargo(context, masterBitstream);
        return (embargoDate != null) ? embargoDate.toString() : null;
    }

    /**
     * Get the master bitstream for an Item. Master bitstream is either the
     * defined item primary bitstream, either the first bitstream of default
     * bundle.
     *
     * @param item: the item to analyze
     * @return the master item bitstream if exists, otherwise return null.
     */
    private Bitstream getMasterBitstreamForItem(@NotNull Item item) {
        List<Bundle> bundles = item.getBundles(Constants.DEFAULT_BUNDLE_NAME);
        Bitstream bitstream = bundles
                .stream()
                .map(Bundle::getPrimaryBitstream)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (bitstream == null) {
            bitstream = bundles
                    .stream()
                    .map(Bundle::getBitstreams)
                    .flatMap(List::stream)
                    .findFirst()
                    .orElse(null);
        }
        return bitstream;
    }

    /**
     * Look at the DSpace object's policies to determine an access status value.
     *
     * If the object is null, returns the "metadata.only" value.
     * If any policy attached to the object is valid for the anonymous group,
     * returns the "open.access" value.
     * Otherwise, if the policy start date is before the embargo threshold date,
     * returns the "embargo" value.
     * Every other cases return the "restricted" value.
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
        return (accessValue != null)
            ? getControlledAccessValue(accessValue)
            : OPEN_ACCESS;
    }

    /**
     * Get the first access rights value into DSpaceObject metadata list.
     *
     * @param dso: The DspaceObject to analyze
     * @return the corresponding access right metadata value, or null if not find.
     */
    private String getAccessFromMetadata(DSpaceObject dso) {
        try {
            DSpaceObjectService<DSpaceObject> service = contentFactory.getDSpaceObjectService(dso);
            return service.getMetadataFirstValue(dso, accessMetadataFieldName, "*");
        } catch (UnsupportedOperationException uoe){
            return UNKNOWN;
        }
    }

    /**
     * Retrieve the embargo date about a bitstream checking the related resource policies.
     *
     * @param context    the DSpace context
     * @param bitstream  the DSpace bitstream to analyze
     * @return the corresponding embargo start date if bitstream is embargoed.
     * @throws SQLException
     */
    private Date retrieveEmbargo(Context context, Bitstream bitstream) throws SQLException {
        List<ResourcePolicy> policies = uclouvainResourcePolicyService.find(context, bitstream);
        ResourcePolicy masterPolicy = uclouvainResourcePolicyService.getMasterPolicy(policies);
        return (masterPolicy != null && masterPolicy.getRpName().equals(EMBARGO))
            ? masterPolicy.getStartDate()
            : null;
    }

    /** Convert a access value string to a controlled vocabulary entry
     *
     * @param initialValue the access value to convert.
     * @return the converted access value.
     */
    public static String getControlledAccessValue(String initialValue) {
        switch (initialValue.trim().toLowerCase()) {
            case "openaccess": return OPEN_ACCESS;
            case "administrator": return ADMINISTRATOR;
            case "embargo": return EMBARGO;
            default: return RESTRICTED;
        }
    }
}
