package org.dspace.app.rest.model;

import java.util.UUID;

import org.dspace.app.rest.RestResourceController;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The BitstreamDirectDownloadURL REST Resource.
 * This class is used to return the download URL of a bitstream.
 * 
 * @author Michaël Pourbaix <michael.pourbaix@uclouvain.be>
 */
public class BitstreamDirectDownloadURLRest extends BaseObjectRest<UUID> {
    public static final String NAME = "bitstreamdirectdownloadurl";
    public static final String CATEGORY = RestAddressableModel.CORE;

    private String url;

    public String getUrl() {return url;}
    public void setUrl(String url) {this.url = url;}

    @JsonIgnore
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    @JsonIgnore
    public Class getController() {
        return RestResourceController.class;
    }
}
