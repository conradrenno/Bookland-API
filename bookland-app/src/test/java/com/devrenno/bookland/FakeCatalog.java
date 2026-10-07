package com.devrenno.bookland;

import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.Book;
import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.BookCatalogGrpc;
import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.GetBooksRequest;
import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.GetBooksResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The catalog service, as the monolith sees it, now that the real one runs in its own process
 * (bookland-catalog-app, where it has its own tests). It honours the two contracts the monolith
 * speaks with the catalog, and nothing more:
 *
 * <ul>
 *   <li><b>gRPC</b> {@code BookCatalog.GetBooks} — served by the in-memory gRPC server of the test
 *       context, which is where every channel goes in the tests;</li>
 *   <li><b>Kafka</b> — the saga participant: {@code ReserveStock} (all lines or none, one reservation
 *       per order), {@code ReleaseStock} and {@code OrderCancelled}, with the replies the orchestrator
 *       expects on {@code bookland.catalog.stock-replies}.</li>
 * </ul>
 *
 * <p>Books and stock live in memory. Each test creates its own books, so tests never share one.
 * {@link #unreachableFor} makes GetBooks fail for a request naming a given book — the catalog "down"
 * for one test's data only; keep it to a couple of calls, or the clients' circuit breakers (shared by
 * the whole context) would open for everyone.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeCatalog extends BookCatalogGrpc.BookCatalogImplBase {

    private static final String STOCK_REPLIES = "bookland.catalog.stock-replies";

    private final Map<UUID, FakeBook> books = new ConcurrentHashMap<>();
    private final Map<UUID, Reservation> reservations = new ConcurrentHashMap<>();
    private final Set<UUID> unreachable = ConcurrentHashMap.newKeySet();
    private final ObjectMapper json = new ObjectMapper();
    private final KafkaTemplate<String, String> kafkaTemplate;

    public FakeCatalog(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // The catalog owns its command and reply topics; with the catalog gone, the tests declare them.
    @Bean
    NewTopic fakeCatalogStockCommandsTopic() {
        return TopicBuilder.name("bookland.catalog.stock-commands").partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic fakeCatalogStockRepliesTopic() {
        return TopicBuilder.name(STOCK_REPLIES).partitions(3).replicas(1).build();
    }

    // --- what a test arranges and reads ---

    public UUID addBook(String price, int stock) {
        UUID id = UUID.randomUUID();
        books.put(id, new FakeBook(id, "Book " + id.toString().substring(0, 8), new BigDecimal(price), stock));
        return id;
    }

    public synchronized int stock(UUID bookId) {
        return books.get(bookId).stock;
    }

    public synchronized void setStock(UUID bookId, int stock) {
        books.get(bookId).stock = stock;
    }

    /** RESERVED, RELEASED, FAILED, or null when the order never reached the catalog. */
    public String reservationStatus(UUID orderId) {
        Reservation reservation = reservations.get(orderId);
        return reservation == null ? null : reservation.status;
    }

    public void unreachableFor(UUID bookId) {
        unreachable.add(bookId);
    }

    // --- gRPC: BookCatalog ---

    @Override
    public void getBooks(GetBooksRequest request, StreamObserver<GetBooksResponse> response) {
        if (request.getBookIdsList().stream().map(UUID::fromString).anyMatch(unreachable::contains)) {
            response.onError(Status.UNAVAILABLE.withDescription("catalog down (fake)").asRuntimeException());
            return;
        }
        GetBooksResponse.Builder answer = GetBooksResponse.newBuilder();
        synchronized (this) {
            for (String id : request.getBookIdsList()) {
                FakeBook book = books.get(UUID.fromString(id));
                if (book != null) {
                    answer.addBooks(Book.newBuilder().setId(id).setTitle(book.title).setCoverImageUrl("")
                            .setPrice(book.price.toPlainString()).setStockQuantity(book.stock).build());
                }
            }
        }
        response.onNext(answer.build());
        response.onCompleted();
    }

    // --- Kafka: the saga participant ---

    @KafkaListener(topics = "bookland.catalog.stock-commands", groupId = "fake-catalog")
    public void onStockCommand(String payload) {
        JsonNode command = json.readTree(payload);
        UUID orderId = UUID.fromString(command.get("orderId").asText());
        switch (command.get("type").asText()) {
            case "ReserveStock" -> reserve(orderId, command.get("items"));
            case "ReleaseStock" -> release(orderId);
            default -> { }
        }
    }

    @KafkaListener(topics = "bookland.orders.order-events", groupId = "fake-catalog")
    public void onOrderEvent(String payload) {
        JsonNode event = json.readTree(payload);
        if ("OrderCancelled".equals(event.get("type").asText())) {
            release(UUID.fromString(event.get("orderId").asText()));
        }
    }

    /** All lines or none; a second ReserveStock for the same order changes nothing. */
    private void reserve(UUID orderId, JsonNode items) {
        List<UUID> unavailable = new ArrayList<>();
        synchronized (this) {
            if (reservations.containsKey(orderId)) {
                return;
            }
            for (JsonNode line : items) {
                FakeBook book = books.get(UUID.fromString(line.get("bookId").asText()));
                if (book == null || book.stock < line.get("quantity").asInt()) {
                    unavailable.add(UUID.fromString(line.get("bookId").asText()));
                }
            }
            Map<UUID, Integer> taken = new ConcurrentHashMap<>();
            if (unavailable.isEmpty()) {
                for (JsonNode line : items) {
                    UUID bookId = UUID.fromString(line.get("bookId").asText());
                    books.get(bookId).stock -= line.get("quantity").asInt();
                    taken.merge(bookId, line.get("quantity").asInt(), Integer::sum);
                }
            }
            reservations.put(orderId, new Reservation(unavailable.isEmpty() ? "RESERVED" : "FAILED", taken));
        }
        String type = unavailable.isEmpty() ? "StockReserved" : "StockReservationFailed";
        kafkaTemplate.send(STOCK_REPLIES, orderId.toString(), json.writeValueAsString(Map.of(
                "messageId", UUID.randomUUID().toString(), "type", type, "orderId", orderId.toString(),
                "unavailableBookIds", unavailable.stream().map(UUID::toString).toList())));
    }

    /** Units go back once, and only from a reservation still RESERVED. */
    private synchronized void release(UUID orderId) {
        Reservation reservation = reservations.get(orderId);
        if (reservation == null || !"RESERVED".equals(reservation.status)) {
            return;
        }
        reservation.items.forEach((bookId, quantity) -> books.get(bookId).stock += quantity);
        reservation.status = "RELEASED";
    }

    private static final class FakeBook {
        final UUID id;
        final String title;
        final BigDecimal price;
        int stock;

        FakeBook(UUID id, String title, BigDecimal price, int stock) {
            this.id = id;
            this.title = title;
            this.price = price;
            this.stock = stock;
        }
    }

    private static final class Reservation {
        volatile String status;
        final Map<UUID, Integer> items;

        Reservation(String status, Map<UUID, Integer> items) {
            this.status = status;
            this.items = items;
        }
    }
}
