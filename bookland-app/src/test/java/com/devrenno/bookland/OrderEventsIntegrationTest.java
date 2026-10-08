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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * What orders announces on {@code bookland.orders.order-events}: one event per status change anyone
 * outside orders cares about, each carrying enough for a consumer to need nothing else — the
 * customer's email and name as the token had them at checkout, the items, the total, the reason.
 * That is what the notification service writes its emails from.
 *
 * <p>Read from the orders outbox, where each event is written in the transaction that changed the
 * order; {@code published_at} being stamped is the relay confirming the broker took it — a type the
 * relay has no topic for would stay pending and hold back every message behind it.
 */
@BooklandIntegrationTest
class OrderEventsIntegrationTest {

    private static final Duration SAGA = Duration.ofSeconds(30);
    private static final String NAME = "Ana Reader";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JWKSource<SecurityContext> jwkSource;
    @Autowired private FakeCatalog catalog;

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
        bookId = catalog.addBook("40.00", 100);
    }

    @Test
    @DisplayName("confirmed: one OrderConfirmed with the customer's contact, the items and the total — nothing for the steps before")
    void confirmedOrderIsAnnounced() throws Exception {
        UUID customerId = UUID.randomUUID();
        String token = tokens.forCaller(customerId, "CUSTOMER", NAME);
        UUID orderId = checkout(token, 2);

        JsonNode event = awaitEvent(orderId, "OrderConfirmed");

        assertThat(event.get("orderId").asText()).isEqualTo(orderId.toString());
        assertThat(event.get("customerId").asText()).isEqualTo(customerId.toString());
        assertThat(event.get("customerEmail").asText()).isEqualTo("customer@bookland.com");
        assertThat(event.get("customerName").asText()).isEqualTo(NAME);
        assertThat(event.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(event.get("totalAmount").decimalValue()).isEqualByComparingTo("80.00");
        assertThat(event.get("items")).hasSize(1);
        JsonNode item = event.get("items").get(0);
        assertThat(item.get("bookId").asText()).isEqualTo(bookId.toString());
        assertThat(item.get("title").asText()).startsWith("Book ");
        assertThat(item.get("quantity").asInt()).isEqualTo(2);
        assertThat(item.get("unitPrice").decimalValue()).isEqualByComparingTo("40.00");
        assertThat(event.get("occurredAt").asText()).isNotBlank();

        assertThat(eventTypes(orderId)).containsExactly("OrderConfirmed");
    }

    @Test
    @DisplayName("declined: OrderPaymentFailed with the reason")
    void paymentFailureIsAnnounced() throws Exception {
        UUID orderId = checkout(tokens.forCaller(UUID.randomUUID(), "CUSTOMER", NAME), 30);

        JsonNode event = awaitEvent(orderId, "OrderPaymentFailed");

        assertThat(event.get("status").asText()).isEqualTo("PAYMENT_FAILED");
        assertThat(event.get("reason").asText()).isNotBlank();
        assertThat(event.get("customerEmail").asText()).isEqualTo("customer@bookland.com");
    }

    /** Both pass the courtesy check on the last copy; the reservation turns one of them down. */
    @Test
    @DisplayName("no stock at reservation: OrderRejected naming the book")
    void rejectionIsAnnounced() throws Exception {
        String first = tokens.forCaller(UUID.randomUUID(), "CUSTOMER", NAME);
        String second = tokens.forCaller(UUID.randomUUID(), "CUSTOMER", NAME);
        addToCart(first, 1);
        addToCart(second, 1);
        catalog.setStock(bookId, 1);

        UUID a = orderIdOf(postCheckout(first));
        UUID b = orderIdOf(postCheckout(second));

        await().atMost(SAGA).untilAsserted(() -> assertThat(List.of(statusOf(a), statusOf(b)))
                .containsExactlyInAnyOrder("CONFIRMED", "REJECTED"));
        UUID rejected = "REJECTED".equals(statusOf(a)) ? a : b;

        JsonNode event = awaitEvent(rejected, "OrderRejected");
        assertThat(event.get("reason").asText()).contains(bookId.toString());
        assertThat(eventTypes(rejected)).containsExactly("OrderRejected");
    }

    @Test
    @DisplayName("shipped by the admin: OrderShipped; delivered: nothing more")
    void shipmentIsAnnouncedAndDeliveryIsNot() throws Exception {
        UUID orderId = checkout(tokens.forCaller(UUID.randomUUID(), "CUSTOMER", NAME), 1);
        awaitEvent(orderId, "OrderConfirmed");

        moveTo(orderId, "SHIPPED");
        moveTo(orderId, "DELIVERED");

        JsonNode event = awaitEvent(orderId, "OrderShipped");
        assertThat(event.get("status").asText()).isEqualTo("SHIPPED");
        assertThat(event.get("customerName").asText()).isEqualTo(NAME);
        assertThat(eventTypes(orderId)).containsExactly("OrderConfirmed", "OrderShipped");
    }

    /** The cancellation the catalog and payments act on now carries the customer too. */
    @Test
    @DisplayName("cancelled by the customer: OrderCancelled with the customer's contact")
    void cancellationCarriesTheCustomer() throws Exception {
        String token = tokens.forCaller(UUID.randomUUID(), "CUSTOMER", NAME);
        UUID orderId = checkout(token, 1);
        awaitEvent(orderId, "OrderConfirmed");

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        JsonNode event = awaitEvent(orderId, "OrderCancelled");
        assertThat(event.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(event.get("customerEmail").asText()).isEqualTo("customer@bookland.com");
        assertThat(eventTypes(orderId)).containsExactly("OrderConfirmed", "OrderCancelled");
    }

    /** Waits for the event to be in the outbox and published, and returns its payload. */
    private JsonNode awaitEvent(UUID orderId, String type) {
        await().atMost(SAGA).untilAsserted(() -> assertThat(jdbcTemplate.queryForObject(
                "select count(*) from orders_outbox where aggregate_id = ? and event_type = ? and published_at is not null",
                Integer.class, orderId, type)).isOne());
        String payload = jdbcTemplate.queryForObject(
                "select payload from orders_outbox where aggregate_id = ? and event_type = ?",
                String.class, orderId, type);
        JsonNode event = json.readTree(payload);
        assertThat(event.get("type").asText()).isEqualTo(type);
        assertThat(event.get("messageId").asText()).isNotBlank();
        return event;
    }

    /** The order's events in the order they were written; saga commands are left out. */
    private List<String> eventTypes(UUID orderId) {
        return jdbcTemplate.queryForList(
                "select event_type from orders_outbox where aggregate_id = ? and event_type like 'Order%' order by created_at",
                String.class, orderId);
    }

    private void moveTo(UUID orderId, String newStatus) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/orders/{id}/status", orderId)
                        .header("Authorization", "Bearer " + tokens.forCaller(UUID.randomUUID(), "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus": "%s"}
                                """.formatted(newStatus)))
                .andExpect(status().isOk());
    }

    private UUID checkout(String token, int quantity) throws Exception {
        addToCart(token, quantity);
        return orderIdOf(postCheckout(token));
    }

    private void addToCart(String token, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/cart/items").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": "%s", "quantity": %d}
                                """.formatted(bookId, quantity)))
                .andExpect(status().is2xxSuccessful());
    }

    private ResultActions postCheckout(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/cart/checkout").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"paymentMethod": "CREDIT_CARD"}
                                """))
                .andExpect(status().isAccepted());
    }

    private UUID orderIdOf(ResultActions result) throws Exception {
        return UUID.fromString(json.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText());
    }

    private String statusOf(UUID orderId) {
        return jdbcTemplate.queryForObject("select status from orders where id = ?", String.class, orderId);
    }
}
