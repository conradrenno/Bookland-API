package com.devrenno.bookland;

import com.devrenno.bookland.catalog.infrastructure.grpc.orders.v1.HasActiveOrdersRequest;
import com.devrenno.bookland.catalog.infrastructure.grpc.orders.v1.HasActiveOrdersResponse;
import com.devrenno.bookland.catalog.infrastructure.grpc.orders.v1.OrderActivityGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.boot.test.context.TestComponent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The orders service, as far as the catalog sees it: answers {@code HasActiveOrders} from a set the
 * test fills, or does not answer at all. A bean of a gRPC service type, so the in-memory gRPC server
 * of the test context serves it next to the real BookCatalog.
 *
 * <p>Keyed by book, so a test only affects the books it created.
 */
@TestComponent
public class FakeOrderActivity extends OrderActivityGrpc.OrderActivityImplBase {

    private final Set<UUID> booksWithActiveOrders = ConcurrentHashMap.newKeySet();
    private final Set<UUID> unanswered = ConcurrentHashMap.newKeySet();

    public void activeOrdersFor(UUID bookId) {
        booksWithActiveOrders.add(bookId);
    }

    /** Asked about this book, orders fails as if it were down. */
    public void downFor(UUID bookId) {
        unanswered.add(bookId);
    }

    @Override
    public void hasActiveOrders(HasActiveOrdersRequest request, StreamObserver<HasActiveOrdersResponse> response) {
        UUID bookId = UUID.fromString(request.getBookId());
        if (unanswered.contains(bookId)) {
            response.onError(Status.UNAVAILABLE.withDescription("orders is down (fake)").asRuntimeException());
            return;
        }
        response.onNext(HasActiveOrdersResponse.newBuilder().setActive(booksWithActiveOrders.contains(bookId)).build());
        response.onCompleted();
    }
}
