package com.devrenno.bookland.reviews.api.controller;

import com.devrenno.bookland.reviews.api.dto.request.CreateReviewRequest;
import com.devrenno.bookland.reviews.application.dto.CreateReviewCommand;
import com.devrenno.bookland.reviews.application.dto.ReviewListResponse;
import com.devrenno.bookland.reviews.application.dto.ReviewResponse;
import com.devrenno.bookland.reviews.application.port.in.CreateReviewUseCase;
import com.devrenno.bookland.reviews.application.port.in.ListReviewsUseCase;
import com.devrenno.bookland.reviews.application.port.in.ModerateReviewUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/books/{bookId}/reviews")
@RequiredArgsConstructor
public class BookReviewController {

    private final CreateReviewUseCase createReviewUseCase;
    private final ListReviewsUseCase listReviewsUseCase;
    private final ModerateReviewUseCase moderateReviewUseCase;

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @PathVariable UUID bookId,
            @Valid @RequestBody CreateReviewRequest request,
            Principal principal
    ) {
        UUID customerId = extractUserId(principal);
        ReviewResponse response = createReviewUseCase.execute(
                new CreateReviewCommand(bookId, customerId, request.rating(), request.comment())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ReviewListResponse> list(
            @PathVariable UUID bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(listReviewsUseCase.execute(bookId, PageRequest.of(page, size)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> moderate(@PathVariable UUID bookId, @PathVariable UUID reviewId) {
        moderateReviewUseCase.execute(reviewId);
        return ResponseEntity.noContent().build();
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
