package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.common.PageQuery;
import com.devrenno.bookland.reviews.application.common.PageResult;
import com.devrenno.bookland.reviews.application.dto.ReviewList;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The listing has no {@code CustomerNamePort} at all any more — its factory does not take one, so it
 * cannot call the user module even by accident. What is left to pin is that the name it shows is
 * the one stored on the review.
 */
@ExtendWith(MockitoExtension.class)
class ListReviewsServiceTest {

    @Mock private ReviewPersistencePort reviewPersistencePort;

    private final UUID bookId = UUID.randomUUID();

    @Test
    void execute_shouldLabelEachReviewWithTheNameStoredOnIt() {
        Review ana = review("Ana Souza", 5);
        Review unresolved = review(null, 3);
        PageQuery pageQuery = new PageQuery(0, 10);
        when(reviewPersistencePort.findByBookId(bookId, pageQuery))
                .thenReturn(new PageResult<>(List.of(ana, unresolved), 0, 10, 2, 1));
        when(reviewPersistencePort.findAllActiveByBookId(bookId)).thenReturn(List.of(ana, unresolved));

        ReviewList result = ListReviewsService.create(reviewPersistencePort).execute(bookId, pageQuery);

        assertThat(result.reviews().content())
                .extracting("customerName")
                .containsExactly("Ana Souza", null);
        assertThat(result.averageRating()).isEqualTo(4.0);
    }

    private Review review(String customerName, int rating) {
        return Review.reconstitute(UUID.randomUUID(), bookId, UUID.randomUUID(), customerName, rating,
                null, Instant.now(), false);
    }
}
