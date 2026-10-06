package com.devrenno.bookland;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The checkout's steps in their saga order, against the real database: the stock is reserved in the
 * catalog before the order is charged, and a declined charge gives the reservation back.
 *
 * <p>The unit tests prove the order of the calls against fakes; this proves the rows. A reservation
 * that was recorded but whose units never went back, or a stock that moved twice, would pass them.
 *
 * <p>The decline is triggered with the simulated gateway's limit (1000.00 by default): a stock large
 * enough to buy 30 copies is set on a seeded book, so the total crosses it for any seeded price.
 */
@BooklandIntegrationTest
class CheckoutReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JWKSource<SecurityContext> jwkSource;

    @Value("${bookland.resource-server.issuer}")
    private String issuer;

    @Value("${bookland.resource-server.audience}")
    private String apiAudience;

    private final ObjectMapper json = new ObjectMapper();

    private TestAccessTokens tokens;
    private UUID bookId;

    @BeforeEach
    void setUp() {
        tokens = new TestAccessTokens(jwkSource, issuer, apiAudience);
        bookId = jdbcTemplate.queryForObject("""
                select id from books where active = true and price >= 34.00 order by isbn limit 1
                """, UUID.class);
        jdbcTemplate.update("update books set stock_quantity = 100 where id = ?", bookId);
    }

    @Test
    @DisplayName("an approved checkout reserves the units and confirms the order")
    void approvedCheckoutKeepsTheReservation() throws Exception {
        String token = customerWithInCart(1);

        String body = checkout(token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andReturn().getResponse().getContentAsString();
        UUID orderId = UUID.fromString(json.readTree(body).get("id").asText());

        assertThat(stock()).isEqualTo(99);
        assertThat(reservationStatus(orderId)).isEqualTo("RESERVED");
    }

    /** The compensation: the units the reservation took are back, and the order keeps the reason. */
    @Test
    @DisplayName("a declined checkout releases the reservation and keeps the cart")
    void declinedCheckoutGivesTheStockBack() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = tokens.forCaller(customerId, "CUSTOMER");
        addToCart(token, 30);

        checkout(token)
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("PAYMENT_DECLINED"));

        assertThat(stock()).as("units given back by the compensation").isEqualTo(100);
        UUID orderId = jdbcTemplate.queryForObject(
                "select id from orders where customer_id = ?", UUID.class, customerId);
        assertThat(jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, orderId))
                .isEqualTo("PAYMENT_FAILED");
        assertThat(jdbcTemplate.queryForObject("select status_reason from orders where id = ?", String.class, orderId))
                .isNotBlank();
        assertThat(reservationStatus(orderId)).isEqualTo("RELEASED");
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from carts where customer_id = ?", Integer.class, customerId))
                .as("the cart stays, so the customer can try again").isOne();
    }

    private String customerWithInCart(int quantity) throws Exception {
        String token = tokens.forCaller(UUID.randomUUID(), "CUSTOMER");
        addToCart(token, quantity);
        return token;
    }

    private void addToCart(String token, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "quantity": %d}
                                """.formatted(bookId, quantity)))
                .andExpect(status().is2xxSuccessful());
    }

    private ResultActions checkout(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/cart/checkout").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"paymentMethod": "CREDIT_CARD"}
                        """));
    }

    private int stock() {
        return jdbcTemplate.queryForObject("select stock_quantity from books where id = ?", Integer.class, bookId);
    }

    private String reservationStatus(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "select status from stock_reservations where order_id = ?", String.class, orderId);
    }
}
