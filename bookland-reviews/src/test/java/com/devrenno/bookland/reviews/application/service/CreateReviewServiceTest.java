package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.application.dto.ReviewView;
import com.devrenno.bookland.reviews.application.port.out.BookExistsPort;
import com.devrenno.bookland.reviews.application.port.out.BookRatingUpdatePort;
import com.devrenno.bookland.reviews.application.port.out.CustomerNamePort;
import com.devrenno.bookland.reviews.application.port.out.PurchaseVerificationPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.DuplicateReviewException;
import com.devrenno.bookland.reviews.domain.exception.PurchaseRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
    @Mock private BookRatingUpdatePort bookRatingUpdatePort;
    @Mock private CustomerNamePort customerNamePort;

    private CreateReviewService service;

    private final UUID bookId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = CreateReviewService.create(reviewPersistencePort, bookExistsPort,
                purchaseVerificationPort, bookRatingUpdatePort, customerNamePort);
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
        verify(bookRatingUpdatePort).updateRating(eq(bookId), anyDouble());
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
    }

    private Review buildReview(int rating) {
        return Review.reconstitute(UUID.randomUUID(), bookId, customerId, rating, null, LocalDateTime.now(), false);
    }
}
