package com.devrenno.bookland.reviews.application.port.in;

import com.devrenno.bookland.reviews.application.common.PageQuery;
import com.devrenno.bookland.reviews.application.dto.ReviewList;

import java.util.UUID;

public interface ListReviewsUseCase {
    ReviewList execute(UUID bookId, PageQuery pageQuery);
}
