package org.dspace.uclouvain.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;

public class GenericHttpClient {
    private HttpClient httpClient; 
    private String baseUrl;
    private String token;

    public GenericHttpClient() {
        this.httpClient = HttpClient.newHttpClient();
    }

    private HttpRequest.Builder generateHttpRequestBuilder(String url) {
        HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder().uri(URI.create(url));
        if(!(this.token == null) && !(this.token.isEmpty())){
            httpRequestBuilder.setHeader("Authorization", this.token);
        }                                 
        return httpRequestBuilder;
    }

    public HttpResponse<String> get(String url) throws IOException, InterruptedException {
        String requestUrl = this.baseUrl + url;

        HttpRequest.Builder requestBuilder = this.generateHttpRequestBuilder(requestUrl);
        requestBuilder.setHeader("accept", "application/json");
        HttpRequest request = requestBuilder.build();

        return this.httpClient.send(request, BodyHandlers.ofString()); 
    }

    public HttpResponse<String> get(String url, HashMap<String, String> headers) throws IOException, InterruptedException {
        String requestUrl = this.baseUrl + url;

        HttpRequest.Builder requestBuilder = generateHttpRequestBuilder(requestUrl);
        for (Map.Entry<String, String> entry: headers.entrySet()){
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
}
