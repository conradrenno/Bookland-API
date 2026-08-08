package com.devrenno.bookland.wishlist.infrastructure.web;

import com.devrenno.bookland.websupport.security.AuthenticatedUser;
import com.devrenno.bookland.wishlist.adapters.controller.WishlistController;
import com.devrenno.bookland.wishlist.adapters.viewmodel.WishlistViewModel;
import com.devrenno.bookland.wishlist.infrastructure.web.dto.AddWishlistItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
public class WishlistApiController {

    private final WishlistController wishlistController;

    @GetMapping
    public ResponseEntity<WishlistViewModel> get(AuthenticatedUser caller) {
        return ResponseEntity.ok(wishlistController.get(caller.id()));
    }

    @PostMapping("/items")
    public ResponseEntity<WishlistViewModel> addItem(
            @Valid @RequestBody AddWishlistItemRequest request,
            AuthenticatedUser caller
    ) {
        return ResponseEntity.ok(wishlistController.addItem(caller.id(), request.bookId()));
    }

    @DeleteMapping("/items/{bookId}")
    public ResponseEntity<WishlistViewModel> removeItem(
            @PathVariable UUID bookId,
            AuthenticatedUser caller
    ) {
        return ResponseEntity.ok(wishlistController.removeItem(caller.id(), bookId));
    }

    @PostMapping("/items/{bookId}/move-to-cart")
    public ResponseEntity<Void> moveToCart(
            @PathVariable UUID bookId,
            AuthenticatedUser caller
    ) {
        wishlistController.moveToCart(caller.id(), bookId);
        return ResponseEntity.ok().build();
    }
}
