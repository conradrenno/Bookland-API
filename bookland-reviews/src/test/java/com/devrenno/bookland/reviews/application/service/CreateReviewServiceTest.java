package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.reviews.application.dto.BookRatingChanged;
import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.application.dto.ReviewView;
import com.devrenno.bookland.reviews.application.port.out.BookExistsPort;
import com.devrenno.bookland.reviews.application.port.out.BookRatingEventPort;
import com.devrenno.bookland.reviews.application.port.out.CustomerNamePort;
import com.devrenno.bookland.reviews.application.port.out.PurchaseVerificationPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.DuplicateReviewException;
import com.devrenno.bookland.reviews.domain.exception.PurchaseRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateReviewServiceTest {

    @Mock private ReviewPersistencePort reviewPersistencePort;
    @Mock private BookExistsPort bookExistsPort;
    @Mock private PurchaseVerificationPort purchaseVerificationPort;
    @Mock private BookRatingEventPort bookRatingEventPort;
    @Mock private CustomerNamePort customerNamePort;

    private final FakeTransactionPort transactionPort = new FakeTransactionPort();

    private CreateReviewService service;

    private final UUID bookId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = CreateReviewService.create(reviewPersistencePort, bookExistsPort,
                purchaseVerificationPort, bookRatingEventPort, customerNamePort, transactionPort);
    }

    @Test
    void execute_shouldCreateReviewAndReturnIt_whenAllConditionsMet() {
        CreateReviewCommand command = new CreateReviewCommand(bookId, customerId, 5, "Great book!");
        Review saved = buildReview(5);

        when(bookExistsPort.exists(bookId)).thenReturn(true);
        when(purchaseVerificationPort.hasPurchasedBook(customerId, bookId)).thenReturn(true);
        when(reviewPersistencePort.findByBookIdAndCustomerId(bookId, customerId)).thenReturn(Optional.empty());
        when(reviewPersistencePort.save(any())).thenReturn(saved);
        when(reviewPersistencePort.findAllActiveByBookId(bookId)).thenReturn(List.of(saved));
        when(customerNamePort.getCustomerName(customerId)).thenReturn("Ana Souza");

        ReviewView result = service.execute(command);

        assertThat(result.rating()).isEqualTo(5);
        assertThat(result.customerName()).isEqualTo("Ana Souza");
    }

    /** The event carries the resulting average and count over every active review, not just the new one. */
    @Test
    void execute_shouldPublishTheBooksNewRating() {
        CreateReviewCommand command = new CreateReviewCommand(bookId, customerId, 5, null);
        when(bookExistsPort.exists(bookId)).thenReturn(true);
        when(purchaseVerificationPort.hasPurchasedBook(customerId, bookId)).thenReturn(true);
        when(reviewPersistencePort.findByBookIdAndCustomerId(bookId, customerId)).thenReturn(Optional.empty());
        when(reviewPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reviewPersistencePort.findAllActiveByBookId(bookId)).thenReturn(List.of(buildReview(5), buildReview(2)));
        when(customerNamePort.getCustomerName(customerId)).thenReturn("Ana Souza");

        service.execute(command);

        ArgumentCaptor<BookRatingChanged> published = ArgumentCaptor.forClass(BookRatingChanged.class);
        verify(bookRatingEventPort).publish(published.capture());
        assertThat(published.getValue().bookId()).isEqualTo(bookId);
        assertThat(published.getValue().averageRating()).isEqualTo(3.5);
        assertThat(published.getValue().reviewCount()).isEqualTo(2);
        assertThat(published.getValue().eventId()).isNotNull();
    }

    /** The event goes to the outbox; only inside the review's transaction are the two stored together. */
    @Test
    void execute_shouldSaveTheReviewAndPublishItsEventInOneTransaction() {
        CreateReviewCommand command = new CreateReviewCommand(bookId, customerId, 4, null);
        when(bookExistsPort.exists(bookId)).thenReturn(true);
        when(purchaseVerificationPort.hasPurchasedBook(customerId, bookId)).thenReturn(true);
        when(reviewPersistencePort.findByBookIdAndCustomerId(bookId, customerId)).thenReturn(Optional.empty());
        when(customerNamePort.getCustomerName(customerId)).thenReturn("Ana Souza");
        when(reviewPersistencePort.save(any())).thenAnswer(invocation -> {
            assertThat(transactionPort.isActive()).as("review saved inside the transaction").isTrue();
            return invocation.getArgument(0);
        });
        doAnswer(invocation -> {
            assertThat(transactionPort.isActive()).as("event published inside the transaction").isTrue();
            return null;
        }).when(bookRatingEventPort).publish(any());

        service.execute(command);

        verify(bookRatingEventPort).publish(any());
    }

    /**
     * The name is looked up once, at creation, and written onto the review — that is what lets the
     * listing stop asking the user module for every author.
     */
    @Test
    void execute_shouldStoreTheAuthorsNameOnTheReview() {
        CreateReviewCommand command = new CreateReviewCommand(bookId, customerId, 4, null);
        when(bookExistsPort.exists(bookId)).thenReturn(true);
        when(purchaseVerificationPort.hasPurchasedBook(customerId, bookId)).thenReturn(true);
        when(reviewPersistencePort.findByBookIdAndCustomerId(bookId, customerId)).thenReturn(Optional.empty());
        when(reviewPersistencePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerNamePort.getCustomerName(customerId)).thenReturn("Ana Souza");

        service.execute(command);

        ArgumentCaptor<Review> stored = ArgumentCaptor.forClass(Review.class);
        verify(reviewPersistencePort).save(stored.capture());
        assertThat(stored.getValue().getCustomerName()).isEqualTo("Ana Souza");
    }

    @Test
    void execute_shouldThrowBookNotFoundException_whenBookDoesNotExist() {
        CreateReviewCommand command = new CreateReviewCommand(bookId, customerId, 5, null);
        when(bookExistsPort.exists(bookId)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(BookNotFoundException.class);

        verify(reviewPersistencePort, never()).save(any());
    }

    @Test
    void execute_shouldThrowPurchaseRequired_whenNoPurchase() {
        CreateReviewCommand command = new CreateReviewCommand(bookId, customerId, 4, null);
        when(bookExistsPort.exists(bookId)).thenReturn(true);
        when(purchaseVerificationPort.hasPurchasedBook(customerId, bookId)).thenReturn(false);

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(PurchaseRequiredException.class);
    }

    @Test
    void execute_shouldThrowDuplicate_whenReviewAlreadyExists() {
        CreateReviewCommand command = new CreateReviewCommand(bookId, customerId, 3, null);
        Review existing = buildReview(3);

        when(bookExistsPort.exists(bookId)).thenReturn(true);
        when(purchaseVerificationPort.hasPurchasedBook(customerId, bookId)).thenReturn(true);
        when(reviewPersistencePort.findByBookIdAndCustomerId(bookId, customerId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(DuplicateReviewException.class);

        verifyNoInteractions(bookRatingEventPort);
    }

    private Review buildReview(int rating) {
        return Review.reconstitute(UUID.randomUUID(), bookId, customerId, "Ana Souza", rating, null,
                Instant.now(), false);
    }
}
