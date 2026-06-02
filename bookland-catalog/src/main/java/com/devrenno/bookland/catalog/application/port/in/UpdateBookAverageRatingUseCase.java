package com.devrenno.bookland.catalog.application.port.in;

import java.util.UUID;

public interface UpdateBookAverageRatingUseCase {
    void updateAverageRating(UUID bookId, double newAvgRating);
}
