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

    private GenericHttpClient httpClient;

    public void setHttpClient(GenericHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public PatoritySearchResult search(PatoritySearchQuery query) {
        if (query == null || !query.hasTerm()) {
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
}