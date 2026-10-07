package com.devrenno.bookland.wishlist.infrastructure.grpc;

import com.devrenno.bookland.wishlist.application.dto.WishlistBookInfo;
import com.devrenno.bookland.wishlist.application.port.out.CatalogUnavailableException;
import com.devrenno.bookland.wishlist.application.port.out.WishlistBookInfoPort;
import com.devrenno.bookland.wishlist.domain.exception.BookNotFoundException;
import com.devrenno.bookland.wishlist.infrastructure.grpc.catalog.v1.Book;
import com.devrenno.bookland.wishlist.infrastructure.grpc.catalog.v1.BookCatalogGrpc;
import com.devrenno.bookland.wishlist.infrastructure.grpc.catalog.v1.GetBooksRequest;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Reads books from the catalog over gRPC for the wishlist: deadline, circuit breaker, and every
 * failure turned into {@link CatalogUnavailableException}. The wishlist's own copy of the client the
 * orders module also has — each service keeps its own, as with the outbox and the inbox — with its
 * own breaker, so the wishlist's view of the catalog does not depend on the cart's traffic.
 */
@Component
public class CatalogGrpcWishlistBookInfoAdapter implements WishlistBookInfoPort {

    private static final Logger log = LoggerFactory.getLogger(CatalogGrpcWishlistBookInfoAdapter.class);

    private final BookCatalogGrpc.BookCatalogBlockingStub stub;
    private final Duration deadline;
    private final CircuitBreaker circuitBreaker;

    public CatalogGrpcWishlistBookInfoAdapter(GrpcChannelFactory channels,
                                              @Value("${bookland.grpc.catalog.deadline:2s}") Duration deadline,
                                              @Value("${bookland.grpc.catalog.circuit-breaker.open-for:10s}") Duration openFor) {
        this.stub = BookCatalogGrpc.newBlockingStub(channels.createChannel("catalog"));
        this.deadline = deadline;
        this.circuitBreaker = CircuitBreaker.of("wishlist-catalog", CircuitBreakerConfig.custom()
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
    public WishlistBookInfo getBookInfo(UUID bookId) {
        WishlistBookInfo book = findBookInfos(List.of(bookId)).get(bookId);
        if (book == null) {
            throw new BookNotFoundException(bookId);
        }
        return book;
    }

    @Override
    public Map<UUID, WishlistBookInfo> findBookInfos(Collection<UUID> bookIds) {
        if (bookIds.isEmpty()) {
            return Map.of();
        }
        GetBooksRequest request = GetBooksRequest.newBuilder()
                .addAllBookIds(bookIds.stream().distinct().map(UUID::toString).toList())
                .build();
        List<Book> books;
        try {
            books = circuitBreaker.executeSupplier(() -> stub
                    .withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                    .getBooks(request)).getBooksList();
        } catch (CallNotPermittedException e) {
            throw new CatalogUnavailableException("The catalog is unavailable (circuit breaker open)", e);
        } catch (RuntimeException e) {
            throw new CatalogUnavailableException("The catalog could not be reached: " + e.getMessage(), e);
        }
        Map<UUID, WishlistBookInfo> found = new HashMap<>();
        for (Book book : books) {
            found.put(UUID.fromString(book.getId()), new WishlistBookInfo(book.getTitle(),
                    book.getCoverImageUrl().isEmpty() ? null : book.getCoverImageUrl(),
                    new BigDecimal(book.getPrice()), book.getStockQuantity(), book.getStockQuantity() > 0));
        }
        return found;
    }
}
