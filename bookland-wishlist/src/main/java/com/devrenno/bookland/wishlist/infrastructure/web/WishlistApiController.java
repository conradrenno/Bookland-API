package com.devrenno.bookland.wishlist.infrastructure.web;

import com.devrenno.bookland.wishlist.adapters.controller.WishlistController;
import com.devrenno.bookland.wishlist.adapters.viewmodel.WishlistViewModel;
import com.devrenno.bookland.wishlist.infrastructure.web.dto.AddWishlistItemRequest;
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
public class WishlistApiController {

    private final WishlistController wishlistController;

    @GetMapping
    public ResponseEntity<WishlistViewModel> get(Principal principal) {
        return ResponseEntity.ok(wishlistController.get(extractUserId(principal)));
    }

    @PostMapping("/items")
    public ResponseEntity<WishlistViewModel> addItem(
            @Valid @RequestBody AddWishlistItemRequest request,
            Principal principal
    ) {
        return ResponseEntity.ok(wishlistController.addItem(extractUserId(principal), request.bookId()));
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<WishlistViewModel> removeItem(
            @PathVariable UUID bookId,
            Principal principal
    ) {
        return ResponseEntity.ok(wishlistController.removeItem(extractUserId(principal), bookId));
    }

    @PostMapping("/items/{bookId}/move-to-cart")
    public ResponseEntity<Void> moveToCart(
            @PathVariable UUID bookId,
            Principal principal
    ) {
        wishlistController.moveToCart(extractUserId(principal), bookId);
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
