/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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

    static List<Integer> acceptedEvents = Arrays.asList(Event.CREATE, Event.MODIFY_METADATA, Event.MODIFY);
    private MetadataFieldName licenseField;
    private String defaultLicenseUrl;
    private boolean enableDefault;
    private BitstreamService bitstreamService;

    @Override
    public void initialize() throws Exception {
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

        ConfigurationService configService = DSpaceServicesFactory.getInstance().getConfigurationService();
        defaultLicenseUrl = configService.getProperty("bitstream.upload.default.license.url");
        enableDefault = configService.getBooleanProperty("bitstream.upload.default.license.enabled", false);
        licenseField = new MetadataFieldName(configService.getProperty(
                "uclouvain.global.metadata.license.field", "dc.rights.license"));
    }

    @Override
    public void consume(Context context, Event event) throws Exception {
        // Only process bitstreams that have no license && are being modified
        if (isEventValid(event) && enableDefault && defaultLicenseUrl != null) {
            Bitstream bitstream = (Bitstream) event.getSubject(context);
            if (bitstreamService.getMetadataFirstValue(bitstream, licenseField, null) == null) {
                bitstreamService.setMetadataSingleValue(context, bitstream, licenseField, null, defaultLicenseUrl);
            }
        }
    }

    @Override
    public void end(Context context) throws Exception {}

    @Override
    public void finish(Context context) throws Exception {}

    private boolean isEventValid(Event event) {
        return event.getSubjectType() == Constants.BITSTREAM && acceptedEvents.contains(event.getEventType());
    }
}
