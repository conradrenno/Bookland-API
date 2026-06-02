package com.devrenno.bookland.wishlist.api.controller;

import com.devrenno.bookland.wishlist.api.dto.request.AddWishlistItemRequest;
import com.devrenno.bookland.wishlist.application.dto.WishlistResponse;
import com.devrenno.bookland.wishlist.application.port.in.AddWishlistItemUseCase;
import com.devrenno.bookland.wishlist.application.port.in.GetWishlistUseCase;
import com.devrenno.bookland.wishlist.application.port.in.MoveToCartUseCase;
import com.devrenno.bookland.wishlist.application.port.in.RemoveWishlistItemUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final GetWishlistUseCase getWishlistUseCase;
    private final AddWishlistItemUseCase addWishlistItemUseCase;
    private final RemoveWishlistItemUseCase removeWishlistItemUseCase;
    private final MoveToCartUseCase moveToCartUseCase;

    @GetMapping
    public ResponseEntity<WishlistResponse> get(Principal principal) {
        return ResponseEntity.ok(getWishlistUseCase.execute(extractUserId(principal)));
    }

    @PostMapping("/items")
    public ResponseEntity<WishlistResponse> addItem(
            @Valid @RequestBody AddWishlistItemRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(addWishlistItemUseCase.execute(extractUserId(principal), request.bookId()));
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<WishlistResponse> removeItem(
            @PathVariable UUID bookId,
            Principal principal
    ) {
        return ResponseEntity.ok(removeWishlistItemUseCase.execute(extractUserId(principal), bookId));
    }

    @PostMapping("/items/{bookId}/move-to-cart")
    public ResponseEntity<Void> moveToCart(
            @PathVariable UUID bookId,
            Principal principal
    ) {
        moveToCartUseCase.execute(extractUserId(principal), bookId);
        return ResponseEntity.ok().build();
    }

    private UUID extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getDetails() instanceof UUID userId) {
            return userId;
        }
        return null;
    }
}
