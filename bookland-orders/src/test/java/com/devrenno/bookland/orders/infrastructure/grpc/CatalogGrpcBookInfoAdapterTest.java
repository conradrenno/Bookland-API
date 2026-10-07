package com.devrenno.bookland.orders.infrastructure.grpc;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.port.out.CatalogUnavailableException;
import com.devrenno.bookland.orders.domain.exception.BookNotFoundException;
import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.Book;
import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.BookCatalogGrpc;
import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.GetBooksRequest;
import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.GetBooksResponse;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.grpc.client.ChannelBuilderOptions;
import org.springframework.grpc.client.GrpcChannelFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The orders gRPC client against a fake catalog living in memory (grpc-inprocess): a real gRPC call,
 * with a server this test controls — answering, slow, failing — to see the deadline, the circuit
 * breaker and the mapping to the module's own exceptions do what the adapter says they do.
 */
class CatalogGrpcBookInfoAdapterTest {

    private static final Duration DEADLINE = Duration.ofMillis(200);

    private final List<ManagedChannel> channels = new ArrayList<>();
    private Server server;

    @AfterEach
    void tearDown() {
        channels.forEach(ManagedChannel::shutdownNow);
        if (server != null) {
            server.shutdownNow();
        }
    }

    @Test
    @DisplayName("one call for many ids; a book the catalog does not return is absent, and getBookInfo throws")
    void batchAndAbsence() throws Exception {
        UUID known = UUID.randomUUID();
        UUID unknown = UUID.randomUUID();
        FakeCatalog catalog = new FakeCatalog();
        catalog.books.add(Book.newBuilder().setId(known.toString()).setTitle("Clean Code")
                .setCoverImageUrl("").setPrice("49.90").setStockQuantity(3).build());
        CatalogGrpcBookInfoAdapter adapter = adapterTo(start(catalog));

        Map<UUID, BookInfo> found = adapter.findBookInfos(List.of(known, unknown));

        assertThat(catalog.calls).hasValue(1);
        assertThat(found).containsOnlyKeys(known);
        BookInfo book = found.get(known);
        assertThat(book.price()).isEqualByComparingTo("49.90");
        assertThat(book.coverImageUrl()).as("empty string on the wire is no cover").isNull();
        assertThatThrownBy(() -> adapter.getBookInfo(unknown)).isInstanceOf(BookNotFoundException.class);
    }

    /** A slow catalog costs the deadline, not the catalog's time. */
    @Test
    @DisplayName("a catalog slower than the deadline: CatalogUnavailableException after about the deadline")
    void deadline() throws Exception {
        FakeCatalog catalog = new FakeCatalog();
        catalog.delayMillis = 3_000;
        CatalogGrpcBookInfoAdapter adapter = adapterTo(start(catalog));

        long start = System.nanoTime();
        assertThatThrownBy(() -> adapter.findBookInfos(List.of(UUID.randomUUID())))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasMessageContaining("DEADLINE_EXCEEDED");
        long tookMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertThat(tookMillis).as("bounded by the 200 ms deadline, far from the 3 s delay").isLessThan(1_500);
    }

    @Test
    @DisplayName("no catalog at the address: CatalogUnavailableException")
    void noCatalog() {
        CatalogGrpcBookInfoAdapter adapter = adapterTo("nobody-listens-" + UUID.randomUUID());

        assertThatThrownBy(() -> adapter.findBookInfos(List.of(UUID.randomUUID())))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasMessageContaining("UNAVAILABLE");
    }

    /**
     * Five failures out of five calls open the breaker (minimum 5 calls, 50% threshold); from then on
     * calls fail at once, without reaching the catalog at all.
     */
    @Test
    @DisplayName("after repeated failures the breaker opens: calls fail at once and never reach the catalog")
    void circuitBreakerOpens() throws Exception {
        FakeCatalog catalog = new FakeCatalog();
        catalog.fail = true;
        CatalogGrpcBookInfoAdapter adapter = adapterTo(start(catalog));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> adapter.findBookInfos(List.of(UUID.randomUUID())))
                    .isInstanceOf(CatalogUnavailableException.class);
        }
        assertThat(catalog.calls).hasValue(5);

        assertThatThrownBy(() -> adapter.findBookInfos(List.of(UUID.randomUUID())))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasMessageContaining("circuit breaker open");
        assertThat(catalog.calls).as("the open breaker did not let the call out").hasValue(5);
    }

    // --- harness ---

    private String start(FakeCatalog catalog) throws Exception {
        String name = "catalog-" + UUID.randomUUID();
        server = InProcessServerBuilder.forName(name).addService(catalog).build().start();
        return name;
    }

    /** A channel factory reduced to the one thing the adapter asks: a channel to the in-memory server. */
    private CatalogGrpcBookInfoAdapter adapterTo(String serverName) {
        GrpcChannelFactory factory = new GrpcChannelFactory() {
            @Override
            public boolean supports(String target) {
                return true;
            }

            @Override
            public ManagedChannel createChannel(String target, ChannelBuilderOptions options) {
                ManagedChannel channel = InProcessChannelBuilder.forName(serverName).build();
                channels.add(channel);
                return channel;
            }
        };
        return new CatalogGrpcBookInfoAdapter(factory, DEADLINE, Duration.ofSeconds(30));
    }

    private static final class FakeCatalog extends BookCatalogGrpc.BookCatalogImplBase {
        final List<Book> books = new ArrayList<>();
        final AtomicInteger calls = new AtomicInteger();
        volatile long delayMillis;
        volatile boolean fail;

        @Override
        public void getBooks(GetBooksRequest request, StreamObserver<GetBooksResponse> response) {
            calls.incrementAndGet();
            if (fail) {
                response.onError(Status.INTERNAL.withDescription("catalog broke").asRuntimeException());
                return;
            }
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            response.onNext(GetBooksResponse.newBuilder().addAllBooks(books.stream()
                    .filter(b -> request.getBookIdsList().contains(b.getId())).toList()).build());
            response.onCompleted();
        }
    }
}
