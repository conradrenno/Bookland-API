package com.devrenno.bookland.wishlist.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddWishlistItemRequest(@NotNull UUID bookId) {}
