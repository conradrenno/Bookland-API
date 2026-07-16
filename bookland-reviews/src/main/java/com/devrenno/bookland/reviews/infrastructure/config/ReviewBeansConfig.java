package com.devrenno.bookland.reviews.infrastructure.config;

import com.devrenno.bookland.reviews.adapters.controller.ReviewController;
import com.devrenno.bookland.reviews.application.port.out.BookExistsPort;
import com.devrenno.bookland.reviews.application.port.out.BookRatingUpdatePort;
import com.devrenno.bookland.reviews.application.port.out.PurchaseVerificationPort;
import com.devrenno.bookland.reviews.application.port.out.ReviewPersistencePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the reviews module. Builds the framework-free inner graph from the outbound
 * ports (implemented by Spring adapters) and exposes the internal ReviewController as a bean.
 */
@Configuration
public class ReviewBeansConfig {

    @Bean
    public ReviewController reviewController(ReviewPersistencePort reviewPersistencePort,
                                             BookExistsPort bookExistsPort,
                                             PurchaseVerificationPort purchaseVerificationPort,
                                             BookRatingUpdatePort bookRatingUpdatePort) {
        return ReviewController.create(reviewPersistencePort, bookExistsPort,
                purchaseVerificationPort, bookRatingUpdatePort);
    }
}
