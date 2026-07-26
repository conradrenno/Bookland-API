package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.dto.ReviewView;
import com.devrenno.bookland.reviews.application.port.out.CustomerNamePort;
import com.devrenno.bookland.reviews.domain.entity.Review;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Labels reviews with their author's display name. A lookup failure degrades to a null name so a
 * missing customer never breaks the listing; names are memoised per call because the same customer
 * often appears more than once in a page.
 */
final class ReviewViewAssembler {

    private final CustomerNamePort customerNamePort;
    private final Map<UUID, String> cache = new HashMap<>();

    ReviewViewAssembler(CustomerNamePort customerNamePort) {
        this.customerNamePort = customerNamePort;
    }

    ReviewView toView(Review review) {
        String name = cache.computeIfAbsent(review.getCustomerId(), this::lookup);
        return new ReviewView(
                review.getId(), review.getBookId(), review.getCustomerId(), name,
                review.getRating(), review.getComment(), review.getCreatedAt()
        );
    }

    private String lookup(UUID customerId) {
        try {
            return customerNamePort.getCustomerName(customerId);
        } catch (Exception e) {
            return null;
        }
    }
}
