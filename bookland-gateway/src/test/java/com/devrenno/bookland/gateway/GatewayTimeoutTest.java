package com.devrenno.bookland.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The gateway when the service behind it fails — the two failures measured in step 5d against the
 * compose stack, here with stand-ins: the API's address is a server that accepts and never answers
 * (a frozen service), the catalog's a port nothing listens on (a stopped one).
 *
 * <p>Before the timeouts, the frozen case held the request for as long as the caller would wait; after
 * them but before {@link UpstreamFailureResolver}, it ended as Boot's generic 500 with no code.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayTimeoutTest {

    private static final HttpServer FROZEN = frozen();
    private static final int NOTHING_LISTENS = freePort();

    @DynamicPropertySource
    static void failingServices(DynamicPropertyRegistry registry) {
        registry.add("APP_URL", () -> "http://localhost:" + FROZEN.getAddress().getPort());
        registry.add("CATALOG_URL", () -> "http://localhost:" + NOTHING_LISTENS);
        registry.add("GATEWAY_READ_TIMEOUT", () -> "1s");
    }

    @AfterAll
    static void stop() {
        FROZEN.stop(0);
    }

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("a frozen service: 504 UPSTREAM_TIMEOUT in problem+json, after the read timeout")
    void frozenService() throws Exception {
        long start = System.nanoTime();
        HttpResponse<String> response = get("/api/v1/cart");
        long tookMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(response.statusCode()).isEqualTo(504);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/problem+json");
        assertThat(response.body()).contains("\"code\":\"UPSTREAM_TIMEOUT\"").contains("\"instance\":\"/api/v1/cart\"");
        assertThat(tookMillis).as("bounded by the 1 s read timeout").isBetween(900L, 5_000L);
    }

    @Test
    @DisplayName("a stopped service: 502 UPSTREAM_UNAVAILABLE in problem+json, at once")
    void stoppedService() throws Exception {
        HttpResponse<String> response = get("/api/v1/books");

        assertThat(response.statusCode()).isEqualTo(502);
        assertThat(response.headers().firstValue("Content-Type")).hasValue("application/problem+json");
        assertThat(response.body()).contains("\"code\":\"UPSTREAM_UNAVAILABLE\"");
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static HttpServer frozen() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", exchange -> {
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A port that was free a moment ago, and is closed again: connections to it are refused. */
    private static int freePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
