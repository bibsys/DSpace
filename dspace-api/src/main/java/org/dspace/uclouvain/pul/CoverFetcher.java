/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.pul;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Downloads a cover image referenced by an ONIX record from the PUL website. Kept apart so that tests can
 * substitute a local source for the network.
 */
public class CoverFetcher {

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(TIMEOUT)
        .build();

    /**
     * @param url the image URL.
     * @return the image bytes.
     * @throws IOException if the download fails or does not return an image.
     */
    public InputStream open(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET().build();
        HttpResponse<InputStream> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while downloading " + url, e);
        }
        String contentType = response.headers().firstValue("Content-Type").orElse("");
        if (response.statusCode() != 200 || !contentType.startsWith("image/")) {
            response.body().close();
            throw new IOException("HTTP %d, content type '%s' for %s".formatted(response.statusCode(), contentType,
                url));
        }
        return response.body();
    }
}
