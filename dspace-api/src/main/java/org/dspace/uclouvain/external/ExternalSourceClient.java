/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * Minimal HTTP GET client for the external sources (CrossRef, PubMed, arXiv). Unlike the upstream
 * {@code LiveImportClient}, which turns every failure into an empty string, it tells "not found" (null) apart from
 * "unavailable" ({@link ExternalSourceException}) and applies a real connection and socket timeout.
 */
public class ExternalSourceClient {

    private final int timeoutMillis;

    public ExternalSourceClient(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Perform a GET request.
     *
     * @param url The URL to fetch.
     * @return The response body, or null when the source answers 404.
     * @throws ExternalSourceException on any other non-2xx status, on timeout and on network error.
     */
    public String get(String url) {
        HttpGet request = new HttpGet(url);
        request.setConfig(RequestConfig.custom()
            .setConnectTimeout(timeoutMillis)
            .setSocketTimeout(timeoutMillis)
            .setConnectionRequestTimeout(timeoutMillis)
            .build());
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            if (status == Status.NOT_FOUND.getStatusCode()) {
                return null;
            }
            if (Family.familyOf(status) != Family.SUCCESSFUL) {
                throw new ExternalSourceException("GET " + url + " answered HTTP " + status + " "
                    + response.getStatusLine().getReasonPhrase());
            }
            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExternalSourceException("GET " + url + " failed: " + e.getMessage(), e);
        }
    }
}
