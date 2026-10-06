package com.devrenno.bookland;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * Who may call which route, for every route of the API, pinned against the real filter chain.
 *
 * <p>Two things are checked, and the first is what makes the second trustworthy:
 *
 * <ul>
 *   <li><strong>The matrix is complete.</strong> The routes are read from Spring MVC itself, so a
 *       new endpoint without a row here fails the build — nobody can add a route and forget to
 *       decide who may call it. A row whose route no longer exists fails too.</li>
 *   <li><strong>Each row holds</strong>, for a caller with no token, a CUSTOMER and an ADMIN.</li>
 * </ul>
 *
 * <p>Only the security decision is asserted, never the business outcome: the requests carry random
 * ids and no body, so an allowed call typically ends in 400 or 404, which is fine. "Allowed" means
 * the answer is neither a 401 nor a 403 {@code INSUFFICIENT_ROLE}. Business 403s such as
 * {@code USER_ACCESS_DENIED} count as allowed — the caller got past the role check and was refused
 * by the module, which is a different decision with a different code.
 *
 * <p>The identity service's half of the matrix; the monolith pins the rest of the API. The OAuth2
 * endpoints are not here: they are served by the Authorization Server's filters, not by Spring MVC
 * handlers, and {@code AuthorizationCodeFlowIntegrationTest} covers them.
 */
@IdentityIntegrationTest
class AccessMatrixIntegrationTest {

    enum Access { PUBLIC, AUTHENTICATED, ADMIN }

    private static final Map<String, Access> MATRIX = Map.ofEntries(
            // auth
            entry("POST /api/v1/auth/register", Access.PUBLIC),
            // user
            entry("GET /api/v1/users/{id}", Access.AUTHENTICATED),
            entry("PUT /api/v1/users/{id}", Access.AUTHENTICATED),
            entry("DELETE /api/v1/users/{id}", Access.AUTHENTICATED)
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

    @Value("${bookland.resource-server.issuer}")
    private String issuer;

    @Value("${bookland.resource-server.audience}")
    private String apiAudience;

    private TestAccessTokens tokens;

    @BeforeEach
    void setUp() {
        tokens = new TestAccessTokens(jwkSource, issuer, apiAudience);
    }

    @Test
    @DisplayName("every API route has a row in the matrix, and every row is a real route")
    void matrixCoversExactlyTheRoutesTheApplicationServes() {
        Set<String> served = new TreeSet<>();
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            for (String pattern : info.getPatternValues()) {
                if (!pattern.startsWith("/api/v1/")) {
                    continue;
                }
                assertThat(info.getMethodsCondition().getMethods())
                        .as("%s answers every HTTP method; give it an explicit one", pattern)
                        .isNotEmpty();
                info.getMethodsCondition().getMethods().forEach(method -> served.add(method + " " + pattern));
            }
        }

        assertThat(served)
                .as("routes served by the application vs rows in the matrix")
                .containsExactlyInAnyOrderElementsOf(MATRIX.keySet());
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("rows")
    void eachRouteAdmitsExactlyTheCallersItShould(String route, Access access) throws Exception {
        MockHttpServletResponse anonymous = call(route, null);
        MockHttpServletResponse customer = call(route, tokens.forRole("CUSTOMER"));
        MockHttpServletResponse admin = call(route, tokens.forRole("ADMIN"));

        if (access == Access.PUBLIC) {
            assertAllowed(route, "no token", anonymous);
        } else {
            assertThat(anonymous.getStatus()).as("%s without a token", route).isEqualTo(401);
        }

        if (access == Access.ADMIN) {
            assertThat(customer.getStatus()).as("%s as CUSTOMER", route).isEqualTo(403);
            assertThat(customer.getContentAsString()).as("%s as CUSTOMER", route).contains("\"INSUFFICIENT_ROLE\"");
        } else {
            assertAllowed(route, "CUSTOMER", customer);
        }

        assertAllowed(route, "ADMIN", admin);
    }

    static Stream<Arguments> rows() {
        return MATRIX.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(row -> Arguments.of(row.getKey(), row.getValue()));
    }

    private MockHttpServletResponse call(String route, String token) throws Exception {
        String[] parts = route.split(" ", 2);
        String uri = parts[1].replaceAll("\\{[^}]+}", UUID.randomUUID().toString());
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(parts[0]), uri);
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(builder).andReturn().getResponse();
    }

    private void assertAllowed(String route, String caller, MockHttpServletResponse response) throws Exception {
        assertThat(response.getStatus()).as("%s as %s", route, caller).isNotEqualTo(401);
        assertThat(response.getContentAsString()).as("%s as %s", route, caller).doesNotContain("\"INSUFFICIENT_ROLE\"");
    }
}
