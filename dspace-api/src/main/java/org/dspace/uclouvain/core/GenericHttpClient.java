/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GenericHttpClient {

    private HttpClient httpClient;
    private String baseUrl;
    private String token;

    // OAuth configuration
    private boolean oauthEnabled = false;
    private String oauthUrl;
    private String oauthKey;
    private String oauthSecret;
    private long tokenExpire = 0;

    private static final Logger log = LogManager.getLogger(GenericHttpClient.class);

    public GenericHttpClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    @PostConstruct
    // Execute this after construct because it cause problems in constructor.
    private void generateToken() {
        if (oauthEnabled) {
            // Initialise the token.
            try {
                refreshToken();
            } catch (Exception e) {
                log.error("Exception while refreshing token :: " + e.getMessage());
                throw new RuntimeException("Could not initialize GenericHttpClient token :: " + e.getMessage());
            }
        }
    }

    private HttpRequest.Builder generateHttpRequestBuilder(String url) {
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().uri(URI.create(url));
        if (!(this.token == null) && !(this.token.isEmpty())) {
            httpRequestBuilder.setHeader("Authorization", this.token);
        }
        return httpRequestBuilder;
    }

    // ---------- OAuth Logic ----------
    /**
     * Retrieve a new token using OAuth2 exchange with esb.
     */
    private void refreshToken() throws IOException, InterruptedException {
        String credentials = oauthKey + ":" + oauthSecret;
        String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes());

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(oauthUrl))
            .header("Authorization", "Basic " + basicAuth)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(BodyPublishers.ofString("grant_type=client_credentials"))
            .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.error("Could not retrieve token from esb :: " + response.body());
            throw new RuntimeException(
                "Unable to retrieve oauth token: " + response.statusCode() + " :: " + response.body()
            );
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(response.body());

        String accessToken = json.get("access_token").asText();
        long expiresIn = json.get("expires_in").asLong();

        // Set the expires time to 60 sec before the real expiration to avoid invalid token problems.
        tokenExpire = System.currentTimeMillis() + ((expiresIn - 60) * 1000);
        log.debug("Regenerated a oauth token :: " + accessToken);
        token = "Bearer " + accessToken;
    }

    /**
     * Check that we have a valid token.
     */
    protected boolean isTokenValid() {
        return token != null && !token.isBlank() && (System.currentTimeMillis() < tokenExpire);
    }

    /**
     * Makes a GET Http Request on the given url and returns the response.
     *
     * @param url The url to get.
     * @return The request response.
     * @throws URISyntaxException for any error with URI syntax
     */
    public HttpResponse<String> get(String url) throws IOException, InterruptedException, URISyntaxException {
        if (oauthEnabled && !isTokenValid()) {
            log.debug("No token found or invalid, refreshing");
            refreshToken();
        }
        String requestUrl = this.baseUrl + url;
        HttpRequest.Builder requestBuilder = this.generateHttpRequestBuilder(requestUrl);
        requestBuilder.setHeader("accept", "application/json");
        HttpRequest request = requestBuilder.build();
        return this.httpClient.send(request, BodyHandlers.ofString());
    }

    /**
     * Makes a GET Http Request on the given url with the given headers and returns the response.
     *
     * @param url The url to get.
     * @param headers The headers to add to the request.
     * @return The request response.
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException for any error with URI syntax
     */
    public HttpResponse<String> get(String url, HashMap<String, String> headers)
            throws IOException, InterruptedException, URISyntaxException {
        String requestUrl = this.baseUrl + url;
        HttpRequest.Builder requestBuilder = generateHttpRequestBuilder(requestUrl);
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.setHeader(entry.getKey(), entry.getValue());
        }
        HttpRequest request = requestBuilder.build();
        return this.httpClient.send(request, BodyHandlers.ofString());
    }

    // Getter && Setters
    public String getBaseUrl() {
        return this.baseUrl;
    }
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public void setOauthEnabled(boolean oauthEnabled) {
        this.oauthEnabled = oauthEnabled;
    }
    public void setOauthUrl(String oauthUrl) {
        this.oauthUrl = oauthUrl;
    }
    public void setOauthKey(String oauthKey) {
        this.oauthKey = oauthKey;
    }
    public void setOauthSecret(String oauthSecret) {
        this.oauthSecret = oauthSecret;
    }
}
