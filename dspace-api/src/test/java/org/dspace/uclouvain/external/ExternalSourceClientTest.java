/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.uclouvain.external;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import jakarta.ws.rs.core.Response.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The client must tell "not found" (null) apart from "unavailable" (exception), and must not hang on a slow source.
 */
public class ExternalSourceClientTest {

    private static final int TIMEOUT_MILLIS = 300;

    private HttpServer server;
    private String baseUrl;
    private final ExternalSourceClient client = new ExternalSourceClient(TIMEOUT_MILLIS);

    @Before
    public void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ok", exchange -> {
            byte[] body = "{\"message\": \"hello\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(Status.OK.getStatusCode(), body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.createContext("/missing",
            exchange -> exchange.sendResponseHeaders(Status.NOT_FOUND.getStatusCode(), -1));
        server.createContext("/broken",
            exchange -> exchange.sendResponseHeaders(Status.INTERNAL_SERVER_ERROR.getStatusCode(), -1));
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(TIMEOUT_MILLIS * 4);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(Status.OK.getStatusCode(), -1);
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    @Test
    public void returnsTheBodyOnSuccess() {
        assertEquals("{\"message\": \"hello\"}", client.get(baseUrl + "/ok"));
    }

    @Test
    public void returnsNullWhenNotFound() {
        assertNull(client.get(baseUrl + "/missing"));
    }

    @Test
    public void throwsOnServerError() {
        ExternalSourceException e = assertThrows(ExternalSourceException.class, () -> client.get(baseUrl + "/broken"));
        assertTrue(e.getMessage(), e.getMessage().contains("HTTP " + Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test
    public void throwsOnTimeout() {
        ExternalSourceException e = assertThrows(ExternalSourceException.class, () -> client.get(baseUrl + "/slow"));
        assertTrue(String.valueOf(e.getCause()), e.getCause() instanceof SocketTimeoutException);
    }

    @Test
    public void throwsWhenUnreachable() {
        server.stop(0);
        assertThrows(ExternalSourceException.class, () -> client.get(baseUrl + "/ok"));
    }
}
