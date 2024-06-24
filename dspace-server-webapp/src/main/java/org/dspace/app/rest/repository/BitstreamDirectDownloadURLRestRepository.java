package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.BitstreamDirectDownloadURL;
import org.dspace.app.rest.model.BitstreamDirectDownloadURLRest;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.uclouvain.services.BitstreamDirectDownloadURLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component(BitstreamDirectDownloadURLRest.CATEGORY + "." + BitstreamDirectDownloadURLRest.NAME)
public class BitstreamDirectDownloadURLRestRepository extends DSpaceRestRepository<BitstreamDirectDownloadURLRest, UUID>{
    
    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private BitstreamDirectDownloadURLService bitstreamDirectDownloadURLService;

    /**
     * Main method called when GET on 'api/core/bitstreamdirectdownloadurl/{uuid}' route.
     * We recover the bitstream and generate a download URL for it.
     * 
     * @param context The current DSpace context.
     * @param bitstreamId The UUID of the bitstream to generate url for.
     */
    @PreAuthorize("hasPermission(#bitstreamId, 'BITSTREAM', 'DOWNLOAD_URL')")
    public BitstreamDirectDownloadURLRest findOne(Context context, UUID bitstreamId) {
        // TODO: Duplicate with 'BitstreamDirectDownloadURLLinkRepository.java' see if there is a way to avoid this
        try {
            Bitstream bitstream = this.bitstreamService.find(context, bitstreamId);
            if (bitstream == null) {
                throw new ResourceNotFoundException("No such bitstream: " + bitstreamId);
            } 

            BitstreamDirectDownloadURL bdu = new BitstreamDirectDownloadURL();
            bdu.setBitstreamId(bitstreamId);
            bdu.setUrl(this.bitstreamDirectDownloadURLService.getURL(bitstream, context.getCurrentUser()));

            return converter.toRest(bdu, utils.obtainProjection());
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    public Page<BitstreamDirectDownloadURLRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not allowed!", "");
    }

    @Override
    public Class<BitstreamDirectDownloadURLRest> getDomainClass() {
        return BitstreamDirectDownloadURLRest.class;
    }
}
