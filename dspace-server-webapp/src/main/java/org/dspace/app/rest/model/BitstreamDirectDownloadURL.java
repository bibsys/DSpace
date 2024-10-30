/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.UUID;

/**
 * Class representing the download URL of a bitstream.
 * 
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class BitstreamDirectDownloadURL {

    private UUID bitstreamId;
    private String url;

    public UUID getBitstreamId() {
        return bitstreamId;
    }
    public void setBitstreamId(UUID bitstreamId) {
        this.bitstreamId = bitstreamId;
    }

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
}
