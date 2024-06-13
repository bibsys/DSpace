package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.BitstreamDirectDownloadURL;
import org.dspace.app.rest.model.BitstreamDirectDownloadURLRest;
import org.dspace.app.rest.projection.Projection;
import org.springframework.stereotype.Component;

/**
 * This is the converter from/to the BitstreamDownloadURL in the DSpace API data model and
 * the REST data model.
 * 
 * @Author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
@Component
public class BitstreamDirectDownloadURLConverter implements DSpaceConverter<BitstreamDirectDownloadURL, BitstreamDirectDownloadURLRest> {
    
    @Override
    public BitstreamDirectDownloadURLRest convert(BitstreamDirectDownloadURL bdu, Projection projection) {
        BitstreamDirectDownloadURLRest response = new BitstreamDirectDownloadURLRest();
        response.setUrl(bdu.getUrl());
        response.setId(bdu.getBitstreamId());
        return response;
    }

    @Override
    public Class<BitstreamDirectDownloadURL> getModelClass(){
        return BitstreamDirectDownloadURL.class;
    }
}
