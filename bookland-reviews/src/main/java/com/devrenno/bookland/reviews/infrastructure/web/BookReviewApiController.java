package com.devrenno.bookland.reviews.infrastructure.web;

import com.devrenno.bookland.reviews.adapters.controller.ReviewController;
import com.devrenno.bookland.reviews.adapters.viewmodel.ReviewListViewModel;
import com.devrenno.bookland.reviews.adapters.viewmodel.ReviewViewModel;
import com.devrenno.bookland.reviews.application.common.PageQuery;
import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.infrastructure.web.dto.CreateReviewRequest;
import com.devrenno.bookland.websupport.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books/{bookId}/reviews")
@RequiredArgsConstructor
public class BookReviewApiController {

    private final ReviewController reviewController;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ReviewViewModel> create(
            @PathVariable UUID bookId,
            @Valid @RequestBody CreateReviewRequest request,
            AuthenticatedUser caller
    ) {
        CreateReviewCommand command = new CreateReviewCommand(
                bookId, caller.id(), request.rating(), request.comment()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewController.create(command));
    }

    @GetMapping
    public ResponseEntity<ReviewListViewModel> list(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(reviewController.list(bookId, PageQuery.of(page, size)));
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> moderate(@PathVariable UUID bookId, @PathVariable UUID reviewId) {
        reviewController.moderate(reviewId);
        return ResponseEntity.noContent().build();
    }
}
