package com.devrenno.bookland.reviews.application.port.out;

import com.devrenno.bookland.reviews.application.dto.BookRatingChanged;

/**
 * Announces that a book's rating changed. The reviews module does not know, and must not care, who
 * reacts to it.
 */
public interface BookRatingEventPort {
    void publish(BookRatingChanged event);
}
