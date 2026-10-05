package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.dto.BookRatingChanged;
import com.devrenno.bookland.reviews.application.port.out.BookRatingEventPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.ReviewNotFoundException;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModerateReviewServiceTest {

    @Mock private ReviewPersistencePort reviewPersistencePort;
    @Mock private BookRatingEventPort bookRatingEventPort;

    private ModerateReviewService service;

    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = ModerateReviewService.create(reviewPersistencePort, bookRatingEventPort);
    }

    @Test
    void execute_shouldPublishTheRatingOfTheRemainingReviews() {
        Review moderated = buildReview(1);
        when(reviewPersistencePort.findById(moderated.getId())).thenReturn(Optional.of(moderated));
        when(reviewPersistencePort.findAllActiveByBookId(bookId)).thenReturn(List.of(buildReview(4), buildReview(5)));

        service.execute(moderated.getId());

        BookRatingChanged event = publishedEvent();
        assertThat(event.bookId()).isEqualTo(bookId);
        assertThat(event.averageRating()).isEqualTo(4.5);
        assertThat(event.reviewCount()).isEqualTo(2);
    }

    /** Moderating the last review away must bring the book back to unrated, not leave the old average. */
    @Test
    void execute_shouldPublishAZeroRating_whenTheLastReviewIsRemoved() {
        Review moderated = buildReview(5);
        when(reviewPersistencePort.findById(moderated.getId())).thenReturn(Optional.of(moderated));
        when(reviewPersistencePort.findAllActiveByBookId(bookId)).thenReturn(List.of());

        service.execute(moderated.getId());

        BookRatingChanged event = publishedEvent();
        assertThat(event.averageRating()).isZero();
        assertThat(event.reviewCount()).isZero();
    }

    @Test
    void execute_shouldPublishNothing_whenTheReviewDoesNotExist() {
        UUID reviewId = UUID.randomUUID();
        when(reviewPersistencePort.findById(reviewId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(reviewId)).isInstanceOf(ReviewNotFoundException.class);

        verifyNoInteractions(bookRatingEventPort);
    }

    private BookRatingChanged publishedEvent() {
        ArgumentCaptor<BookRatingChanged> published = ArgumentCaptor.forClass(BookRatingChanged.class);
        verify(bookRatingEventPort).publish(published.capture());
        return published.getValue();
    }

    private Review buildReview(int rating) {
        return Review.reconstitute(UUID.randomUUID(), bookId, UUID.randomUUID(), "Ana Souza", rating, null,
                Instant.now(), false);
    }
}
