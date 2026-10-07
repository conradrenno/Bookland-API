package com.devrenno.bookland.orders.infrastructure.grpc;

import com.devrenno.bookland.orders.application.port.in.CheckActiveOrdersUseCase;
import com.devrenno.bookland.orders.infrastructure.grpc.v1.HasActiveOrdersRequest;
import com.devrenno.bookland.orders.infrastructure.grpc.v1.HasActiveOrdersResponse;
import com.devrenno.bookland.orders.infrastructure.grpc.v1.OrderActivityGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Answers the catalog's "does this book have orders not yet settled?" before it removes a book.
 * Until step 5 orders answered by implementing the catalog's port in process, which made orders
 * depend on the catalog's jar; now it serves the question and knows nothing about who asks.
 */
@Component
public class OrderActivityGrpcService extends OrderActivityGrpc.OrderActivityImplBase {

    private final CheckActiveOrdersUseCase checkActiveOrdersUseCase;

    public OrderActivityGrpcService(CheckActiveOrdersUseCase checkActiveOrdersUseCase) {
        this.checkActiveOrdersUseCase = checkActiveOrdersUseCase;
    }

    @Override
    public void hasActiveOrders(HasActiveOrdersRequest request, StreamObserver<HasActiveOrdersResponse> responseObserver) {
        UUID bookId;
        try {
            bookId = UUID.fromString(request.getBookId());
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
            return;
        }
        responseObserver.onNext(HasActiveOrdersResponse.newBuilder()
                .setActive(checkActiveOrdersUseCase.hasActiveOrdersForBook(bookId))
                .build());
        responseObserver.onCompleted();
    }
}
