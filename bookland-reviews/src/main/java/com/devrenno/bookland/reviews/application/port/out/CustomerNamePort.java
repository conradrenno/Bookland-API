package com.devrenno.bookland.reviews.application.port.out;

import java.util.UUID;

public interface CustomerNamePort {
    /** Display name of the review's author, or null when the customer can no longer be resolved. */
    String getCustomerName(UUID customerId);
}
