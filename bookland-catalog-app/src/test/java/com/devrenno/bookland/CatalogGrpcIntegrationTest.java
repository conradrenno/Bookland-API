package com.devrenno.bookland;

import com.devrenno.bookland.catalog.infrastructure.grpc.v1.Book;
import com.devrenno.bookland.catalog.infrastructure.grpc.v1.BookCatalogGrpc;
import com.devrenno.bookland.catalog.infrastructure.grpc.v1.GetBooksRequest;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The catalog's gRPC in both directions: what it serves ({@code BookCatalog.GetBooks}, called here
 * through a real stub over the in-memory transport, as the other services call it) and what it asks
 * (orders' {@code HasActiveOrders} before a removal, answered by {@link FakeOrderActivity}).
 */
@CatalogIntegrationTest
class CatalogGrpcIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private GrpcChannelFactory channels;
    @Autowired private FakeOrderActivity orders;
    @Autowired private JWKSource<SecurityContext> jwkSource;

    @Value("${bookland.resource-server.issuer}")
    private String issuer;

    @Value("${bookland.resource-server.audience}")
    private String apiAudience;

    private final ObjectMapper json = new ObjectMapper();
    private TestAccessTokens tokens;
    private BookCatalogGrpc.BookCatalogBlockingStub catalog;

    @BeforeEach
    void setUp() {
        tokens = new TestAccessTokens(jwkSource, issuer, apiAudience);
        catalog = BookCatalogGrpc.newBlockingStub(channels.createChannel("catalog"));
    }

    @Test
    @DisplayName("GetBooks: active books in one answer; unknown and removed ones simply absent")
    void getBooks() throws Exception {
        UUID active = newBook("49.90", 3);
        UUID removed = newBook("10.00", 1);
        removeBook(removed);

        List<Book> books = catalog.getBooks(GetBooksRequest.newBuilder()
                .addBookIds(active.toString()).addBookIds(removed.toString())
                .addBookIds(UUID.randomUUID().toString()).build()).getBooksList();

        assertThat(books).singleElement().satisfies(book -> {
            assertThat(book.getId()).isEqualTo(active.toString());
            assertThat(book.getPrice()).isEqualTo("49.90");
            assertThat(book.getStockQuantity()).isEqualTo(3);
            assertThat(book.getCoverImageUrl()).isEmpty();
        });
    }

    @Test
    @DisplayName("GetBooks with a malformed id: INVALID_ARGUMENT")
    void malformedId() {
        assertThatThrownBy(() -> catalog.getBooks(GetBooksRequest.newBuilder().addBookIds("not-a-uuid").build()))
                .isInstanceOfSatisfying(StatusRuntimeException.class,
                        e -> assertThat(e.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT));
    }

    @Test
    @DisplayName("removing a book orders still holds: 409 BOOK_HAS_ACTIVE_ORDERS, asked over gRPC")
    void removalBlockedByOrders() throws Exception {
        UUID bookId = newBook("20.00", 1);
        orders.activeOrdersFor(bookId);

        mockMvc.perform(delete("/api/v1/books/" + bookId).header("Authorization", "Bearer " + tokens.forRole("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BOOK_HAS_ACTIVE_ORDERS"));
    }

    /** Unknown is not "no orders": removing unchecked could strand an order on a removed book. */
    @Test
    @DisplayName("removing a book while orders cannot answer: 503 ORDERS_UNAVAILABLE, book kept")
    void removalRefusedWhenOrdersIsDown() throws Exception {
        UUID bookId = newBook("20.00", 1);
        orders.downFor(bookId);

        mockMvc.perform(delete("/api/v1/books/" + bookId).header("Authorization", "Bearer " + tokens.forRole("ADMIN")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("ORDERS_UNAVAILABLE"));
        assertThat(catalog.getBooks(GetBooksRequest.newBuilder().addBookIds(bookId.toString()).build())
                .getBooksCount()).as("still in the catalog").isOne();
    }

    private UUID newBook(String price, int stock) throws Exception {
        String isbn = "978" + String.format("%010d", (long) (Math.random() * 1e10));
        String body = mockMvc.perform(post("/api/v1/books")
                        .header("Authorization", "Bearer " + tokens.forRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "gRPC %s", "isbn": "%s", "authors": ["Tester"],
                                 "price": %s, "stockQuantity": %d,
                                 "categoryId": "c3d4e5f6-a7b8-9012-cdef-123456789012"}
                                """.formatted(isbn, isbn, price, stock)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asText());
    }

    private void removeBook(UUID bookId) throws Exception {
        mockMvc.perform(delete("/api/v1/books/" + bookId).header("Authorization", "Bearer " + tokens.forRole("ADMIN")))
                .andExpect(status().isNoContent());
    }
}
