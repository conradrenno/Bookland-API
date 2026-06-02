package com.devrenno.bookland.reviews.application.port.out;

import java.util.UUID;

public interface BookRatingUpdatePort {
    void updateRating(UUID bookId, double newAvgRating);
}
