/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.util.Collections;
import java.util.UUID;
import javax.annotation.Nullable;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.BitstreamDirectDownloadURL;
import org.dspace.app.rest.model.BitstreamDirectDownloadURLRest;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.uclouvain.services.DirectLinkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(BitstreamRest.CATEGORY + "." + BitstreamRest.PLURAL_NAME + "." + BitstreamRest.DOWNLOAD_URL)
public class BitstreamDirectDownloadURLLinkRepository
        extends AbstractDSpaceRestRepository
        implements LinkRestRepository {

    @Autowired
    private BitstreamService bitstreamService;
    @Autowired
    private DirectLinkService uclouvainDirectLinkService;

    /**
     * Main method called when GET on 'api/core/bitstream/{uuid}/download_url' route.
     * We recover the bitstream and generate a download URL for it.
     * 
     * @param request The current HTTP request.
     * @param bitstreamId The UUID of the bitstream to generate url for.
     * @param optionalPageable Some pagination information.
     * @param projection The projection to use to convert the response to something REST.
     */
    @PreAuthorize("hasPermission(#bitstreamId, 'BITSTREAM', 'DOWNLOAD_URL')")
    public BitstreamDirectDownloadURLRest getDownloadURL(
            @Nullable HttpServletRequest request,
            UUID bitstreamId,
            @Nullable Pageable optionalPageable,
            Projection projection
    ) {
        try {
            Context context = obtainContext();
            Bitstream bitstream = bitstreamService.find(context, bitstreamId);
            if (bitstream == null) {
                throw new ResourceNotFoundException("No such bitstream: " + bitstreamId);
            }
            BitstreamDirectDownloadURL bdu = new BitstreamDirectDownloadURL();
            bdu.setBitstreamId(bitstreamId);
            bdu.setUrl(uclouvainDirectLinkService.buildURL(
                context,
                bitstream,
                "thesisSupervisor",
                Collections.emptyMap()
            ));
            return converter.toRest(bdu, projection);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
