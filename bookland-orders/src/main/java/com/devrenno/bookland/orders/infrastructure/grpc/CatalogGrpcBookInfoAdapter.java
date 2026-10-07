package com.devrenno.bookland.orders.infrastructure.grpc;

import com.devrenno.bookland.orders.application.dto.BookInfo;
import com.devrenno.bookland.orders.application.port.out.BookInfoPort;
import com.devrenno.bookland.orders.application.port.out.CatalogUnavailableException;
import com.devrenno.bookland.orders.domain.exception.BookNotFoundException;
import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.Book;
import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.BookCatalogGrpc;
import com.devrenno.bookland.orders.infrastructure.grpc.catalog.v1.GetBooksRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.grpc.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads books from the catalog over gRPC ({@code BookCatalog.GetBooks}, in batches). Three guards
 * around every call, because the catalog is now something that can be down or slow:
 *
 * <ul>
 *   <li>a <b>deadline</b>: a slow catalog costs at most that long, instead of holding the customer's
 *       request (and its thread) for as long as the catalog takes;</li>
 *   <li>a <b>circuit breaker</b> (Resilience4j): when most recent calls failed, further calls fail at
 *       once for a while instead of each waiting out its deadline, which also stops piling load on a
 *       catalog that is struggling. Its state changes are logged — an open breaker is an outage;</li>
 *   <li>any failure becomes {@link CatalogUnavailableException}: the application decides, flow by
 *       flow, whether to refuse (add to cart, checkout) or degrade (render the cart).</li>
 * </ul>
 *
 * <p>The channel is the one named {@code catalog} in {@code spring.grpc.client.channels.*}.
 */
@Component
public class CatalogGrpcBookInfoAdapter implements BookInfoPort {

    private static final Logger log = LoggerFactory.getLogger(CatalogGrpcBookInfoAdapter.class);

    private final BookCatalogGrpc.BookCatalogBlockingStub stub;
    private final Duration deadline;
    private final CircuitBreaker circuitBreaker;

    public CatalogGrpcBookInfoAdapter(GrpcChannelFactory channels,
                                      @Value("${bookland.grpc.catalog.deadline:2s}") Duration deadline,
                                      @Value("${bookland.grpc.catalog.circuit-breaker.open-for:10s}") Duration openFor) {
        this.stub = BookCatalogGrpc.newBlockingStub(channels.createChannel("catalog"));
        this.deadline = deadline;
        this.circuitBreaker = CircuitBreaker.of("orders-catalog", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(openFor)
                .permittedNumberOfCallsInHalfOpenState(2)
                .build());
        circuitBreaker.getEventPublisher().onStateTransition(event ->
                log.warn("Circuit breaker to the catalog: {}", event.getStateTransition()));
    }

    @Override
    public BookInfo getBookInfo(UUID bookId) {
        BookInfo book = findBookInfos(List.of(bookId)).get(bookId);
        if (book == null) {
            throw new BookNotFoundException(bookId);
        }
        return book;
    }

    @Override
    public Map<UUID, BookInfo> findBookInfos(Collection<UUID> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        GetBooksRequest request = GetBooksRequest.newBuilder()
                .addAllBookIds(bookIds.stream().distinct().map(UUID::toString).toList())
                .build();
        try {
            return circuitBreaker.executeSupplier(() -> stub
                            .withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                            .getBooks(request))
                    .getBooksList().stream()
                    .map(CatalogGrpcBookInfoAdapter::toInfo)
                    .collect(Collectors.toMap(BookInfo::id, Function.identity()));
        } catch (CallNotPermittedException e) {
            throw new CatalogUnavailableException("The catalog is unavailable (circuit breaker open)", e);
        } catch (RuntimeException e) {
            // Only the gRPC status code reaches the response; the transport's message (addresses,
            // "io exception"...) stays in the cause, for the log.
            throw new CatalogUnavailableException(
                    "The catalog could not be reached (" + Status.fromThrowable(e).getCode() + ")", e);
        }
    }

    private static BookInfo toInfo(Book book) {
        return new BookInfo(UUID.fromString(book.getId()), book.getTitle(),
                book.getCoverImageUrl().isEmpty() ? null : book.getCoverImageUrl(),
                new BigDecimal(book.getPrice()), book.getStockQuantity());
    }
}
