package com.devrenno.bookland.reviews.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewJpaEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "book_id", nullable = false)
    private UUID bookId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean deleted;

    /**
     * The customer while the review is live, null once a moderator removed it: what the unique index
     * {@code uk_reviews_live_review (book_id, live_customer_id)} is on, so a customer has at most one
     * live review per book and can review again after a removal. Derived here, never by the domain.
     */
    @Column(name = "live_customer_id")
    private UUID liveCustomerId;
}
