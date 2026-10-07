package com.devrenno.bookland.reviews.infrastructure.grpc;

import com.devrenno.bookland.reviews.application.port.out.BookExistsPort;
import com.devrenno.bookland.reviews.application.port.out.CatalogUnavailableException;
import com.devrenno.bookland.reviews.infrastructure.grpc.catalog.v1.BookCatalogGrpc;
import com.devrenno.bookland.reviews.infrastructure.grpc.catalog.v1.GetBooksRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Asks the catalog over gRPC whether a book exists before a review is written for it. Same guards as
 * the other modules' catalog clients — deadline, circuit breaker, failures as
 * {@link CatalogUnavailableException} — in the reviews module's own copy.
 */
@Component
public class CatalogGrpcBookExistsAdapter implements BookExistsPort {

    private static final Logger log = LoggerFactory.getLogger(CatalogGrpcBookExistsAdapter.class);

    private final BookCatalogGrpc.BookCatalogBlockingStub stub;
    private final Duration deadline;
    private final CircuitBreaker circuitBreaker;

    public CatalogGrpcBookExistsAdapter(GrpcChannelFactory channels,
                                        @Value("${bookland.grpc.catalog.deadline:2s}") Duration deadline,
                                        @Value("${bookland.grpc.catalog.circuit-breaker.open-for:10s}") Duration openFor) {
        this.stub = BookCatalogGrpc.newBlockingStub(channels.createChannel("catalog"));
        this.deadline = deadline;
        this.circuitBreaker = CircuitBreaker.of("reviews-catalog", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(openFor)
                .permittedNumberOfCallsInHalfOpenState(2)
                .build());
        circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("Circuit breaker to the catalog: {}", event.getStateTransition()));
    }

    /** True when the catalog has the book active; a removed book does not exist, for reviews either. */
    @Override
    public boolean exists(UUID bookId) {
        GetBooksRequest request = GetBooksRequest.newBuilder().addBookIds(bookId.toString()).build();
        try {
            return circuitBreaker.executeSupplier(() -> stub
                    .withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                    .getBooks(request)).getBooksCount() > 0;
        } catch (CallNotPermittedException e) {
            throw new CatalogUnavailableException("The catalog is unavailable (circuit breaker open)", e);
        } catch (RuntimeException e) {
            throw new CatalogUnavailableException("The catalog could not be reached: " + e.getMessage(), e);
        }
    }
}
