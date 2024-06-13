package org.dspace.app.rest.model;

import java.util.UUID;

/**
 * Class representing the download URL of a bitstream.
 * 
 * @Author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class BitstreamDirectDownloadURL {
    private UUID bitstreamId;
    private String url;

    public UUID getBitstreamId(){return bitstreamId;}
    public void setBitstreamId(UUID bitstreamId){this.bitstreamId = bitstreamId;}

    public String getUrl(){return url;}
    public void setUrl(String url){this.url = url;}
}
