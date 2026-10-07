package com.devrenno.bookland.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The gateway against two stand-ins — JDK HTTP servers playing the monolith and the catalog, each
 * answering with its own name, the method, the path and query it received, the Authorization header
 * and the body. So each case shows where the request landed and what arrived there.
 *
 * <p>The case that matters most is the shared prefix: {@code /api/v1/books/{id}/reviews} belongs to
 * the monolith although {@code /api/v1/books/**} would send it to the catalog.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final HttpServer APP = stub("app");
    private static final HttpServer CATALOG = stub("catalog");

    @DynamicPropertySource
    static void routesToTheStubs(DynamicPropertyRegistry registry) {
        registry.add("APP_URL", () -> "http://localhost:" + APP.getAddress().getPort());
        registry.add("CATALOG_URL", () -> "http://localhost:" + CATALOG.getAddress().getPort());
    }

    @AfterAll
    static void stopStubs() {
        APP.stop(0);
        CATALOG.stop(0);
    }

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    @ParameterizedTest(name = "{0} {1} -> {2}")
    @CsvSource({
            "GET,    /api/v1/books,                                   catalog",
            "GET,    /api/v1/books/b1,                                catalog",
            "POST,   /api/v1/books/b1/cover,                          catalog",
            "PATCH,  /api/v1/books/b1/inventory,                      catalog",
            "GET,    /api/v1/books/b1/inventory/history,              catalog",
            "GET,    /api/v1/categories,                              catalog",
            "GET,    /api/v1/categories/c1/books,                     catalog",
            "GET,    /api/v1/inventory/low-stock,                     catalog",
            "GET,    /media/covers/b1.jpg,                            catalog",
            "GET,    /api/v1/books/b1/reviews,                        app",
            "POST,   /api/v1/books/b1/reviews,                        app",
            "DELETE, /api/v1/books/b1/reviews/r1,                     app",
            "GET,    /api/v1/cart,                                    app",
            "POST,   /api/v1/cart/checkout,                           app",
            "GET,    /api/v1/orders,                                  app",
            "PATCH,  /api/v1/admin/orders/o1/status,                  app",
            "GET,    /api/v1/payments/order/o1,                       app",
            "GET,    /api/v1/wishlist,                                app",
    })
    @DisplayName("each path reaches the service that owns it, with method and path unchanged")
    void routes(String method, String path, String expectedService) throws Exception {
        HttpResponse<String> response = send(method, path, null, null);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).startsWith(expectedService + " " + method + " " + path);
    }

    @Test
    @DisplayName("the Bearer token, the query string and the body reach the service untouched")
    void forwardsWhatTheServiceNeeds() throws Exception {
        HttpResponse<String> response = send("POST", "/api/v1/cart/items?x=1", "Bearer abc.def.ghi",
                "{\"bookId\":\"b1\",\"quantity\":2}");

        assertThat(response.body())
                .startsWith("app POST /api/v1/cart/items?x=1")
                .contains("auth=Bearer abc.def.ghi")
                .endsWith("body={\"bookId\":\"b1\",\"quantity\":2}");
    }

    /** Only the API is routed: each service's own pages (Swagger, H2, /error) stay on its own port. */
    @Test
    @DisplayName("a path no route claims is a 404 at the gateway, never forwarded")
    void unroutedPathsStayHere() throws Exception {
        HttpResponse<String> response = send("GET", "/swagger-ui.html", null, null);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).doesNotStartWith("app").doesNotStartWith("catalog");
    }

    private HttpResponse<String> send(String method, String path, String authorization, String body)
            throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body));
        if (authorization != null) {
            request.header("Authorization", authorization);
        }
        if (body != null) {
            request.header("Content-Type", "application/json");
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Answers every request with "<name> <method> <path?query> auth=<header> body=<body>". */
    private static HttpServer stub(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String received = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String answer = name + " " + exchange.getRequestMethod() + " " + exchange.getRequestURI()
                        + " auth=" + exchange.getRequestHeaders().getFirst("Authorization") + " body=" + received;
                byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
