package com.devrenno.bookland;

import com.devrenno.bookland.reviews.application.dto.BookRatingChanged;
import com.devrenno.bookland.reviews.application.port.out.BookRatingEventPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import com.devrenno.bookland.reviews.application.port.out.TransactionPort;
import com.devrenno.bookland.reviews.domain.entity.Review;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The reason the outbox exists: a review and its rating event are stored together or not at all.
 * Runs against the real adapters and a real transaction manager, because that is the whole claim —
 * a mocked port would pass while the outbox row commits on its own.
 */
@BooklandIntegrationTest
class ReviewOutboxAtomicityIntegrationTest {

    @Autowired
    private TransactionPort transactionPort;

    @Autowired
    private ReviewPersistencePort reviewPersistencePort;

    @Autowired
    private BookRatingEventPort bookRatingEventPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("a failure after both writes rolls back the review and its event together")
    void failureRollsBackBothWrites() {
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), "Ana Souza", 4, null);
        BookRatingChanged event = eventFor(review.getBookId());

        assertThatThrownBy(() -> transactionPort.inTransaction(() -> {
            reviewPersistencePort.save(review);
            bookRatingEventPort.publish(event);
            throw new IllegalStateException("fails after both writes");
        })).hasMessage("fails after both writes");

        assertThat(count("select count(*) from reviews where id = ?", review.getId())).isZero();
        assertThat(count("select count(*) from reviews_outbox where id = ?", event.eventId())).isZero();
    }

    @Test
    @DisplayName("both writes commit together")
    void bothWritesCommit() {
        Review review = Review.create(UUID.randomUUID(), UUID.randomUUID(), "Ana Souza", 4, null);
        BookRatingChanged event = eventFor(review.getBookId());

        transactionPort.inTransaction(() -> {
            reviewPersistencePort.save(review);
            bookRatingEventPort.publish(event);
        });

        assertThat(count("select count(*) from reviews where id = ?", review.getId())).isOne();
        assertThat(count("select count(*) from reviews_outbox where id = ?", event.eventId())).isOne();
    }

    /** Committing the event on its own would bring back the two separate writes the outbox removes. */
    @Test
    @DisplayName("publishing outside a transaction is refused, not committed alone")
    void publishingOutsideATransactionIsRefused() {
        BookRatingChanged event = eventFor(UUID.randomUUID());

        assertThatThrownBy(() -> bookRatingEventPort.publish(event))
                .isInstanceOf(IllegalTransactionStateException.class);

        assertThat(count("select count(*) from reviews_outbox where id = ?", event.eventId())).isZero();
    }

    private static BookRatingChanged eventFor(UUID bookId) {
        return new BookRatingChanged(UUID.randomUUID(), bookId, 4.0, 1, Instant.now());
    }

    private int count(String sql, UUID id) {
        return jdbcTemplate.queryForObject(sql, Integer.class, id);
    }
}
