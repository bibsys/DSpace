package org.dspace.uclouvain.consumer;

import java.util.Arrays;
import java.util.List;

import org.dspace.content.Bitstream;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Consumer used to add a default license to a bitstream that does not have one.
 * Only adds one if the metadata field is empty.
 *
 * @version $Revision$
 *
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class LicenseConsumer implements Consumer {

    static List<Integer> acceptedEvents = Arrays.asList(Event.MODIFY_METADATA, Event.MODIFY);
    private MetadataFieldName licenseMetadataFieldName;
    private String defaultLicenseUrl;
    // Services
    private BitstreamService bitstreamService;
    private ConfigurationService configurationService;

    @Override
    public void finish(Context context) throws Exception {}

    @Override
    public void initialize() throws Exception {
        // Retrieve services
        this.bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

        this.defaultLicenseUrl = this.configurationService.getProperty("uclouvain.global.metadata.license.default", "https://creativecommons.org/licenses/by/4.0/");
        String fieldName = this.configurationService.getProperty("uclouvain.global.metadata.license.field", "dc.rights.license");
        this.licenseMetadataFieldName = new MetadataFieldName(fieldName);
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        // Only process bitstreams that have no license && are being modified
        if (event.getSubjectType() == Constants.BITSTREAM && acceptedEvents.contains(event.getEventType())) {
            Bitstream bitstream = (Bitstream) event.getSubject(context);
            if (bitstreamService.getMetadataFirstValue(bitstream, this.licenseMetadataFieldName, null) == null) {
                bitstreamService.setMetadataSingleValue(context, bitstream, this.licenseMetadataFieldName, null, defaultLicenseUrl);
            }
        }
    }

    @Override
    public void end(Context context) throws Exception {
    }
}
