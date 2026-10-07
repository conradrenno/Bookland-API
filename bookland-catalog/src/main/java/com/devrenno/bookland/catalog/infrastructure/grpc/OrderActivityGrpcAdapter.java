package com.devrenno.bookland.catalog.infrastructure.grpc;

import com.devrenno.bookland.catalog.application.port.out.ActiveOrderCheckPort;
import com.devrenno.bookland.catalog.application.port.out.OrdersUnavailableException;
import com.devrenno.bookland.catalog.infrastructure.grpc.orders.v1.HasActiveOrdersRequest;
import com.devrenno.bookland.catalog.infrastructure.grpc.orders.v1.OrderActivityGrpc;
import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Asks orders, over gRPC, whether a book has orders not yet settled — the question the catalog asks
 * before removing a book. Until step 5 orders answered it by implementing this port itself, which
 * made orders depend on the catalog's jar; now the catalog asks, and orders only serves.
 *
 * <p>Every call has a deadline: without one, an orders service that hangs would hold the admin's
 * request — and its thread — for as long as it hangs. No circuit breaker: the question is rare
 * (an admin removing a book), and an unanswered one refuses the removal
 * ({@link OrdersUnavailableException}, 503) instead of guessing.
 *
 * <p>The channel is the one named {@code orders} in {@code spring.grpc.client.channels.*}, so where
 * orders runs is configuration, not code.
 */
@Component
public class OrderActivityGrpcAdapter implements ActiveOrderCheckPort {

    private final OrderActivityGrpc.OrderActivityBlockingStub stub;
    private final Duration deadline;

    public OrderActivityGrpcAdapter(GrpcChannelFactory channels,
                                    @Value("${bookland.grpc.orders.deadline:2s}") Duration deadline) {
        this.stub = OrderActivityGrpc.newBlockingStub(channels.createChannel("orders"));
        this.deadline = deadline;
    }

    @Override
    public boolean hasActiveOrdersForBook(UUID bookId) {
        try {
            return stub.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                    .hasActiveOrders(HasActiveOrdersRequest.newBuilder().setBookId(bookId.toString()).build())
                    .getActive();
        } catch (StatusRuntimeException e) {
            throw new OrdersUnavailableException(
                    "Orders could not say whether the book has active orders: " + e.getStatus().getCode(), e);
        }
    }
}
