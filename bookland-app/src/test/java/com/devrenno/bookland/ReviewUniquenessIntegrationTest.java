package com.devrenno.bookland;

import com.devrenno.bookland.reviews.application.common.PageQuery;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import com.devrenno.bookland.reviews.domain.exception.DuplicateReviewException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What the database itself guarantees about reviews, below the service's own check: the race the
 * check cannot see — two submissions of the same review at the same moment — is played here by
 * saving twice straight through the persistence port, as the second request would after passing the
 * check too.
 */
@BooklandIntegrationTest
class ReviewUniquenessIntegrationTest {

    @Autowired
    private ReviewPersistencePort reviewPersistencePort;

    @Test
    @DisplayName("a second live review by the same customer for the same book: refused by the index as DUPLICATE_REVIEW")
    void oneLiveReviewPerCustomerAndBook() {
        UUID bookId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        reviewPersistencePort.save(Review.create(bookId, customerId, "Ana", 5, "Great"));

        assertThatThrownBy(() -> reviewPersistencePort.save(Review.create(bookId, customerId, "Ana", 4, "Again")))
                .isInstanceOf(DuplicateReviewException.class);
        assertThat(reviewPersistencePort.findAllActiveByBookId(bookId)).hasSize(1);
    }

    /** Why the index is not a plain unique (book_id, customer_id): moderation must not lock the customer out. */
    @Test
    @DisplayName("after a moderator removed the review, the customer can review the book again")
    void removedReviewDoesNotBlockANewOne() {
        UUID bookId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Review first = reviewPersistencePort.save(Review.create(bookId, customerId, "Ana", 1, "Bad"));
        first.softDelete();
        reviewPersistencePort.save(first);

        reviewPersistencePort.save(Review.create(bookId, customerId, "Ana", 4, "Changed my mind"));

        assertThat(reviewPersistencePort.findAllActiveByBookId(bookId)).hasSize(1);
    }

    @Test
    @DisplayName("the reviews of a book are listed newest first")
    void listedNewestFirst() {
        UUID bookId = UUID.randomUUID();
        Instant now = Instant.now();
        Review older = reviewPersistencePort.save(Review.reconstitute(UUID.randomUUID(), bookId, UUID.randomUUID(),
                "Ana", 3, "First", now.minusSeconds(60), false));
        Review newer = reviewPersistencePort.save(Review.reconstitute(UUID.randomUUID(), bookId, UUID.randomUUID(),
                "Bia", 5, "Second", now, false));

        assertThat(reviewPersistencePort.findByBookId(bookId, new PageQuery(0, 10)).content())
                .extracting(Review::getId)
                .containsExactly(newer.getId(), older.getId());
    }
}
