/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external.patority.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.uclouvain.core.GenericHttpClient;
import org.dspace.uclouvain.core.GenericResponse;
import org.dspace.uclouvain.external.patority.model.PatoritySearchQuery;
import org.dspace.uclouvain.external.patority.model.PatoritySearchResult;

/**
 * Client to interact with the Patority API.
 * This bean is declared in {@code config/spring/uclouvain/authority.xml}.
 *
 * @author Michaël Pourbaix (michael.pourbaix@uclouvain.be)
 */
public class PatorityClientImpl implements PatorityClient {

    private static final Logger logger = LogManager.getLogger(PatorityClientImpl.class);

    private static final String SEARCH_ENDPOINT = "/search";
    private static final String WHOAMI_ENDPOINT = "/whoami";

    private GenericHttpClient httpClient;

    private volatile boolean tokenValid = true;

    /**
     * Check if token is valid at class initialization.
     * Only set flag to false when Patority explicitly rejects the token (HTTP 401).
     * Any other failure (timeout, 5xx, unreachable service) leaves it untouched,
     * so a temporary outage does not disable the authority until the next restart.
     */
    @PostConstruct
    public void checkTokenValidity() {
        try {
            HttpResponse<String> response = httpClient.get(WHOAMI_ENDPOINT);
            int status = response.statusCode();
            if (status == 401) {
                tokenValid = false;
                logger.error(
                    "Patority rejected the token, searches are disabled until restart :: {}", response.body());
            } else if (status != 200) {
                logger.warn("Unexpected response while checking Patority token validity [HTTP {}] :: {}",
                    status, response.body());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Patority token validity check interrupted", e);
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            logger.warn("Could not check Patority token validity at service initialization", e);
        }
    }

    @Override
    public PatoritySearchResult search(PatoritySearchQuery query) {
        if (query == null || !query.hasTerm()) {
            return PatoritySearchResult.empty();
        }
        if (!tokenValid) {
            logger.warn("Skipping Patority search because the token was rejected");
            return PatoritySearchResult.empty();
        }
        String url = null;
        try {
            url = query.toUrl(SEARCH_ENDPOINT);
            HttpResponse<String> response = httpClient.get(url);
            if (response.statusCode() != 200) {
                logger.error("Patority search failed [HTTP {}] for '{}' :: {}", response.statusCode(), url,
                    response.body());
                return PatoritySearchResult.empty();
            }
            return new GenericResponse(response.body())
                .extractJsonResponseDataToClass(null, PatoritySearchResult.class);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Patority search interrupted for {}", query, e);
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            logger.error("Error while fetching Patority data for {} (url: '{}')", query, url, e);
        }
        return PatoritySearchResult.empty();
    }

    // SETTER
    public void setHttpClient(GenericHttpClient httpClient) {
        this.httpClient = httpClient;
    }
}