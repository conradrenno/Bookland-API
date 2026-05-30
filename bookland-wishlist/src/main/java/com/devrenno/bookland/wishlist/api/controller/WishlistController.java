package com.devrenno.bookland.wishlist.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    @GetMapping
    public ResponseEntity<Void> placeholder() {
        return ResponseEntity.status(501).build();
    }
}
