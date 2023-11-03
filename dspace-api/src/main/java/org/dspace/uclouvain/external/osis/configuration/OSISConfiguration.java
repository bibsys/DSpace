/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.osis.configuration;

public class OSISConfiguration {
    private String responseDataKey;

    public void setResponseDataKey(String responseDataKey) {
        this.responseDataKey = responseDataKey;
    }

    public String getResponseDataKey() {
        return this.responseDataKey;
    }
}
