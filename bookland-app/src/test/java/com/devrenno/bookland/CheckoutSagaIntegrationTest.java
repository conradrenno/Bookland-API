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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The checkout saga end to end: orders, catalog and payments talking only through Kafka, each
 * through its own outbox and inbox, against the real database and the embedded broker.
 *
 * <p>The checkout answers 202 with the order PENDING; everything after that is asynchronous, so the
 * assertions wait for the saga to settle (Awaitility, never a sleep). Each hop passes through an
 * outbox relay that polls every second, so a full saga takes a few seconds.
 *
 * <p>The decline is the simulated gateway's limit (1000.00 by default): 30 copies of a seeded book
 * priced above 34.00 cross it.
 */
@BooklandIntegrationTest
class CheckoutSagaIntegrationTest {

    private static final Duration SAGA = Duration.ofSeconds(30);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

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
        bookId = jdbcTemplate.queryForObject(
                "select id from books where active = true and price >= 34.00 order by isbn limit 1", UUID.class);
        setStock(100);
    }

    @Test
    @DisplayName("approved: 202 PENDING at once, then CONFIRMED, stock taken, payment approved, cart gone")
    void approvedCheckout() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = tokens.forCaller(customerId, "CUSTOMER");
        addToCart(token, 1);

        UUID orderId = orderIdOf(checkout(token)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/v1/orders/"))));

        awaitStatus(orderId, "CONFIRMED");
        assertThat(stock()).isEqualTo(99);
        assertThat(reservationStatus(orderId)).isEqualTo("RESERVED");
        assertThat(jdbcTemplate.queryForObject("select status from payments where order_id = ?", String.class, orderId))
                .isEqualTo("APPROVED");
        assertThat(cartCount(customerId)).isZero();
    }

    /** The compensation, asynchronously: the release is one more message after the decline. */
    @Test
    @DisplayName("declined: PAYMENT_FAILED with a reason, stock given back, cart kept and free to retry")
    void declinedCheckoutCompensates() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = tokens.forCaller(customerId, "CUSTOMER");
        addToCart(token, 30);

        UUID orderId = orderIdOf(checkout(token).andExpect(status().isAccepted()));

        awaitStatus(orderId, "PAYMENT_FAILED");
        assertThat(jdbcTemplate.queryForObject("select status_reason from orders where id = ?", String.class, orderId))
                .isNotBlank();
        await().atMost(SAGA).untilAsserted(() -> assertThat(reservationStatus(orderId)).isEqualTo("RELEASED"));
        assertThat(stock()).isEqualTo(100);
        assertThat(cartCount(customerId)).isOne();
        assertThat(jdbcTemplate.queryForObject(
                "select checkout_order_id from carts where customer_id = ?", UUID.class, customerId)).isNull();
    }

    /**
     * The race the reservation exists for. Both checkouts pass the courtesy check — one copy is
     * visible to both — and the catalog's atomic reservation decides: one order confirmed, the other
     * rejected with the book named, and nobody charged for a copy that was not there.
     */
    @Test
    @DisplayName("two customers, one copy: one CONFIRMED, one REJECTED, stock never negative")
    void lastCopyGoesToExactlyOneCustomer() throws Exception {
        String first = tokens.forCaller(UUID.randomUUID(), "CUSTOMER");
        String second = tokens.forCaller(UUID.randomUUID(), "CUSTOMER");
        addToCart(first, 1);
        addToCart(second, 1);
        setStock(1);

        UUID a = orderIdOf(checkout(first).andExpect(status().isAccepted()));
        UUID b = orderIdOf(checkout(second).andExpect(status().isAccepted()));

        await().atMost(SAGA).untilAsserted(() -> assertThat(List.of(statusOf(a), statusOf(b)))
                .containsExactlyInAnyOrder("CONFIRMED", "REJECTED"));
        UUID rejected = "REJECTED".equals(statusOf(a)) ? a : b;
        assertThat(jdbcTemplate.queryForObject("select status_reason from orders where id = ?", String.class, rejected))
                .contains(bookId.toString());
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from payments where order_id = ?", Integer.class, rejected)).isZero();
        assertThat(stock()).isZero();
    }

    @Test
    @DisplayName("a second checkout while the first is running: 409 CHECKOUT_IN_PROGRESS")
    void secondCheckoutWhileTheFirstRuns() throws Exception {
        String token = tokens.forCaller(UUID.randomUUID(), "CUSTOMER");
        addToCart(token, 1);

        UUID orderId = orderIdOf(checkout(token).andExpect(status().isAccepted()));
        checkout(token)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CHECKOUT_IN_PROGRESS"));

        awaitStatus(orderId, "CONFIRMED");
    }

    /**
     * At-least-once delivery, provoked on purpose: the same ReserveStock (same message id) twice,
     * then a ReleaseStock on the same key. One partition keeps the three in order, so once the release
     * is applied the duplicate has been consumed too — and the stock moved down once and back once.
     */
    @Test
    @DisplayName("a duplicated stock command is applied once")
    void duplicatedReserveIsAppliedOnce() {
        UUID orderId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        String reserve = """
                {"messageId":"%s","type":"ReserveStock","orderId":"%s","items":[{"bookId":"%s","quantity":3}]}
                """.formatted(messageId, orderId, bookId);

        kafkaTemplate.send("bookland.catalog.stock-commands", orderId.toString(), reserve);
        kafkaTemplate.send("bookland.catalog.stock-commands", orderId.toString(), reserve);
        kafkaTemplate.send("bookland.catalog.stock-commands", orderId.toString(), """
                {"messageId":"%s","type":"ReleaseStock","orderId":"%s"}
                """.formatted(UUID.randomUUID(), orderId));

        await().atMost(SAGA).untilAsserted(() -> assertThat(reservationStatus(orderId)).isEqualTo("RELEASED"));
        assertThat(stock()).as("down once, back once").isEqualTo(100);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from catalog_outbox where aggregate_id = ?", Integer.class, orderId))
                .as("one reply, not two").isOne();
    }

    /**
     * A second charge command with a different message id gets past the inbox; the payment's own
     * idempotency by order id is what keeps the gateway from charging twice.
     */
    @Test
    @DisplayName("two charge commands for one order: one payment")
    void secondChargeForTheSameOrderChargesNothing() {
        UUID orderId = UUID.randomUUID();
        for (int i = 0; i < 2; i++) {
            kafkaTemplate.send("bookland.payments.payment-commands", orderId.toString(), """
                    {"messageId":"%s","type":"ChargePayment","orderId":"%s","customerId":"%s",
                     "amount":10.00,"method":"PIX"}
                    """.formatted(UUID.randomUUID(), orderId, UUID.randomUUID()));
        }

        await().atMost(SAGA).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "select count(*) from payments_outbox where aggregate_id = ?", Integer.class, orderId)).isEqualTo(2));
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from payments where order_id = ?", Integer.class, orderId)).isOne();
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

    private UUID orderIdOf(ResultActions result) throws Exception {
        return UUID.fromString(json.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText());
    }

    private void awaitStatus(UUID orderId, String expected) {
        await().atMost(SAGA).untilAsserted(() -> assertThat(statusOf(orderId)).isEqualTo(expected));
    }

    private String statusOf(UUID orderId) {
        return jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, orderId);
    }

    private String reservationStatus(UUID orderId) {
        List<String> found = jdbcTemplate.queryForList(
                "select status from stock_reservations where order_id = ?", String.class, orderId);
        return found.isEmpty() ? null : found.get(0);
    }

    private void setStock(int quantity) {
        jdbcTemplate.update("update books set stock_quantity = ? where id = ?", quantity, bookId);
    }

    private int stock() {
        return jdbcTemplate.queryForObject("select stock_quantity from books where id = ?", Integer.class, bookId);
    }

    private int cartCount(UUID customerId) {
        return jdbcTemplate.queryForObject("select count(*) from carts where customer_id = ?", Integer.class, customerId);
    }
}
